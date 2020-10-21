package org.opensrp.web.rest;

import static org.opensrp.common.AllConstants.OpenSRPEvent.Event.LOCATION_ID;
import static org.opensrp.common.AllConstants.Stock.DATE_CREATED;
import static org.opensrp.common.AllConstants.Stock.DATE_UPDATED;
import static org.opensrp.common.AllConstants.Stock.IDENTIFIER;
import static org.opensrp.common.AllConstants.Stock.PROVIDERID;
import static org.opensrp.common.AllConstants.Stock.TIMESTAMP;
import static org.opensrp.common.AllConstants.Stock.TO_FROM;
import static org.opensrp.common.AllConstants.Stock.TRANSACTION_TYPE;
import static org.opensrp.common.AllConstants.Stock.VACCINE_TYPE_ID;
import static org.opensrp.common.AllConstants.Stock.VALUE;
import static org.opensrp.web.rest.RestUtils.getIntegerFilter;
import static org.opensrp.web.rest.RestUtils.getStringFilter;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.json.JSONObject;
import org.opensrp.common.AllConstants.BaseEntity;
import org.opensrp.domain.Inventory;
import org.opensrp.domain.Stock;
import org.opensrp.dto.CsvBulkImportDataSummary;
import org.opensrp.dto.FailedRecordSummary;
import org.opensrp.search.StockSearchBean;
import org.opensrp.service.StockService;
import org.opensrp.web.Constants;
import org.smartregister.utils.DateTimeTypeConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.reflect.TypeToken;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping(value = "/rest/stockresource/")
public class StockResource extends RestResource<Stock> {

	private static Logger logger = LoggerFactory.getLogger(StockResource.class.toString());

	private StockService stockService;

	Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
			.registerTypeAdapter(DateTime.class, new DateTimeTypeConverter()).create();

	private static final String SAMPLE_CSV_FILE = "./importsummaryreport.csv";

	@Autowired
	public StockResource(StockService stockService) {
		this.stockService = stockService;
	}

	@Override
	public Stock getByUniqueId(String uniqueId) {
		return stockService.find(uniqueId);
	}

	/**
	 * Fetch all the stocks
	 * 
	 * @param none
	 * @return a map response with stocks, and optionally msg when an error occurs
	 */

	@RequestMapping(value = "/getall", method = RequestMethod.GET)
	protected ResponseEntity<String> getAll() {
		Map<String, Object> response = new HashMap<String, Object>();
		try {
			List<Stock> stocks = new ArrayList<Stock>();
			stocks = stockService.findAllStocks();
			JsonArray stocksArray = (JsonArray) gson.toJsonTree(stocks, new TypeToken<List<Stock>>() {
			}.getType());
			response.put("stocks", stocksArray);
			return new ResponseEntity<>(gson.toJson(response), RestUtils.getJSONUTF8Headers(), HttpStatus.OK);
		} catch (Exception e) {
			response.put("msg", "Error occurred");
			logger.error("", e);
			return new ResponseEntity<>(new Gson().toJson(response), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	private StockSearchBean populateSearchBean(HttpServletRequest request) {
		StockSearchBean searchBean = new StockSearchBean();
		searchBean.setIdentifier(getStringFilter(IDENTIFIER, request));
		searchBean.setStockTypeId(getStringFilter(VACCINE_TYPE_ID, request));
		searchBean.setTransactionType(getStringFilter(TRANSACTION_TYPE, request));
		searchBean.setProviderId(getStringFilter(PROVIDERID, request));
		searchBean.setValue(getStringFilter(VALUE, request));
		searchBean.setDateCreated(getStringFilter(DATE_CREATED, request));
		searchBean.setToFrom(getStringFilter(TO_FROM, request));
		searchBean.setDateUpdated(getStringFilter(DATE_UPDATED, request));
		searchBean.setLocationId(getStringFilter(LOCATION_ID, request));
		return searchBean;
	}

	/**
	 * Fetch stocks ordered by serverVersion ascending order
	 * 
	 * @param request
	 * @return a map response with events, clients and optionally msg when an error
	 *         occurs
	 */
	@RequestMapping(value = "/sync", method = RequestMethod.GET)
	protected ResponseEntity<String> sync(HttpServletRequest request) {
		Map<String, Object> response = new HashMap<String, Object>();

		try {
			StockSearchBean searchBean = populateSearchBean(request);
			String serverVersion = getStringFilter(BaseEntity.SERVER_VERSIOIN, request);
			if (serverVersion != null) {
				searchBean.setServerVersion(Long.valueOf(serverVersion) + 1);
			}
			Integer limit = getIntegerFilter("limit", request);
			if (limit == null || limit.intValue() == 0) {
				limit = 25;
			}

			List<Stock> stocks = new ArrayList<Stock>();
			stocks = stockService.findStocks(searchBean, BaseEntity.SERVER_VERSIOIN, "asc", limit);
			JsonArray stocksArray = (JsonArray) gson.toJsonTree(stocks, new TypeToken<List<Stock>>() {
			}.getType());

			response.put("stocks", stocksArray);

			return new ResponseEntity<>(gson.toJson(response), RestUtils.getJSONUTF8Headers(), HttpStatus.OK);

		} catch (Exception e) {
			response.put("msg", "Error occurred");
			logger.error("", e);
			return new ResponseEntity<>(new Gson().toJson(response), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@SuppressWarnings("unchecked")
	@RequestMapping(headers = { "Accept=application/json" }, method = POST, value = "/add")
	public ResponseEntity<HttpStatus> save(@RequestBody String data) {
			JSONObject syncData = new JSONObject(data);
			if (!syncData.has("stocks")) {
				return new ResponseEntity<>(BAD_REQUEST);
			}
			ArrayList<Stock> stocks = (ArrayList<Stock>) gson.fromJson(syncData.getJSONArray("stocks").toString(),
					new TypeToken<ArrayList<Stock>>() {
					}.getType());
			for (Stock stock : stocks) {
				try {
					stockService.addorUpdateStock(stock);
				} catch (Exception e) {
					logger.error("Stock" + stock.getId() + " failed to sync", e);
				}
			}
		return new ResponseEntity<>(CREATED);
	}

	@Override
	public Stock create(Stock stock) {
		return stockService.addStock(stock);
	}

	@Override
	public List<String> requiredProperties() {
		List<String> p = new ArrayList<>();
		p.add(PROVIDERID);
		p.add(TIMESTAMP);
		return p;
	}

	@Override
	public Stock update(Stock stock) {
		return stockService.mergeStock(stock);
	}

	@Override
	public List<Stock> search(HttpServletRequest request) throws ParseException {

		StockSearchBean searchBean = populateSearchBean(request);

		String serverVersion = getStringFilter(TIMESTAMP, request);
		if (serverVersion != null)
			searchBean.setServerVersion(Long.valueOf(serverVersion));

		if (!StringUtils.isBlank(searchBean.getIdentifier())) {
			Stock stock = stockService.find(searchBean.getIdentifier());
			if (stock == null) {
				return new ArrayList<>();
			}
		}
		return stockService.findStocks(searchBean);
	}

	@Override
	public List<Stock> filter(String query) {
		return stockService.findAllStocks();
	}

	@PostMapping(consumes = { MediaType.APPLICATION_JSON_VALUE,
			MediaType.TEXT_PLAIN_VALUE })
	public ResponseEntity<String> create(@RequestBody Inventory inventory) {
		try {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			String userName = authentication.getName();
			stockService.addInventory(inventory, userName);
			return new ResponseEntity<>(HttpStatus.CREATED);
		}
		catch (IllegalArgumentException e) {
			logger.error(String.format("Exception occurred while updating Inventory: %s", e.getMessage(), e));
			return new ResponseEntity<String>("The request contain illegal argument : " + e.getMessage(), HttpStatus.BAD_REQUEST);
		}
	}

	@PutMapping(consumes = { MediaType.APPLICATION_JSON_VALUE,
			MediaType.TEXT_PLAIN_VALUE })
	public ResponseEntity<String> update(@RequestBody Inventory inventory) {
		try {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			String userName = authentication.getName();
			stockService.updateInventory(inventory, userName);
			return new ResponseEntity<>(HttpStatus.CREATED);
		}
		catch (IllegalArgumentException e) {
			logger.error(String.format("Exception occurred while updating Inventory: %s", e.getMessage()));
			return new ResponseEntity<String>("The request contain illegal argument : " + e.getMessage(), HttpStatus.BAD_REQUEST);
		}
	}

	@DeleteMapping(value = "/{id}", produces = { MediaType.APPLICATION_JSON_VALUE })
	public ResponseEntity<String> delete(@PathVariable(Constants.RestPartVariables.ID) Long id) {
		if (id == null) {
			return new ResponseEntity<>("Stock item id is required", RestUtils.getJSONUTF8Headers(),
					HttpStatus.BAD_REQUEST);
		} else {
			stockService.deleteStock(id);
			return new ResponseEntity<>("Stock deleted successfully", RestUtils.getJSONUTF8Headers(),
					HttpStatus.NO_CONTENT);
		}
	}

	@GetMapping(value = "/servicePointId/{servicePointId}", produces = {
			MediaType.APPLICATION_JSON_VALUE })
	public ResponseEntity<List<Stock>> getStockItemsByServicePoint(@PathVariable String servicePointId) {
		return new ResponseEntity<>(stockService.getStocksByServicePointId(servicePointId),
				RestUtils.getJSONUTF8Headers(),
				HttpStatus.OK);
	}

	@PostMapping(headers = { "Accept=multipart/form-data" }, produces = {
			MediaType.APPLICATION_JSON_VALUE }, value = "/import/inventory")
	public ResponseEntity importInventoryData(@RequestParam("file") MultipartFile file)
			throws IOException {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		String userName = authentication.getName();
		List<Map<String, String>> csvClients = readCSVFile(file);
		CsvBulkImportDataSummary csvBulkImportDataSummary = stockService
				.convertandPersistInventorydata(csvClients, userName);

		generateCSV(csvBulkImportDataSummary);
		File csvFile = new File(SAMPLE_CSV_FILE);

		return ResponseEntity.ok()
				.header("Content-Disposition", "attachment; filename=" + "importsummaryreport" + ".csv")
				.contentLength(csvFile.length())
				.contentType(MediaType.parseMediaType("text/csv"))
				.body(new FileSystemResource(csvFile));
	}

	private List<Map<String, String>> readCSVFile(MultipartFile file) throws IOException {
		List<Map<String, String>> csvClients = new ArrayList<>();
		try (Reader reader = new InputStreamReader(file.getInputStream());
		     CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.withHeader())) {

			List<CSVRecord> records = parser.getRecords();
			for (CSVRecord record : records) {
				csvClients.add(record.toMap());
			}
		}
		return csvClients;
	}

	private void generateCSV(CsvBulkImportDataSummary csvBulkImportDataSummary) {
		try (
				BufferedWriter writer = Files.newBufferedWriter(Paths.get(SAMPLE_CSV_FILE));
				CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT);
		) {
			csvPrinter.printRecord("Total Number of Rows in the CSV ", csvBulkImportDataSummary.getNumberOfCsvRows());
			csvPrinter.printRecord("Rows processed ", csvBulkImportDataSummary.getNumberOfRowsProcessed());
			csvPrinter.printRecord("\n");

			csvPrinter.printRecord("Row Number", "Reason of Failure");
			for (FailedRecordSummary failedRecordSummary : csvBulkImportDataSummary.getFailedRecordSummaryList()) {
				csvPrinter.printRecord(failedRecordSummary.getRowNumber(), failedRecordSummary.getReasonOfFailure());
			}
			csvPrinter.flush();
		}
		catch (Exception e) {
			logger.error("Failed to generate CSV " + e.getMessage(), e);
			throw new RuntimeException("Failed to generate CSV : " + e.getMessage());
		}
	}


}
