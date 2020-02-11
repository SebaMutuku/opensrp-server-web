package org.opensrp.repository.postgres.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.opensrp.domain.postgres.Stock;
import org.opensrp.domain.postgres.StockExample;

public interface StockMapper {

	/**
	 * This method was generated by MyBatis Generator. This method corresponds to the database table core.stock
	 * @mbg.generated  Fri Mar 23 15:56:38 EAT 2018
	 */
	long countByExample(StockExample example);

	/**
	 * This method was generated by MyBatis Generator. This method corresponds to the database table core.stock
	 * @mbg.generated  Fri Mar 23 15:56:38 EAT 2018
	 */
	int deleteByExample(StockExample example);

	/**
	 * This method was generated by MyBatis Generator. This method corresponds to the database table core.stock
	 * @mbg.generated  Fri Mar 23 15:56:38 EAT 2018
	 */
	int deleteByPrimaryKey(Long id);

	/**
	 * This method was generated by MyBatis Generator. This method corresponds to the database table core.stock
	 * @mbg.generated  Fri Mar 23 15:56:38 EAT 2018
	 */
	int insert(Stock record);

	/**
	 * This method was generated by MyBatis Generator. This method corresponds to the database table core.stock
	 * @mbg.generated  Fri Mar 23 15:56:38 EAT 2018
	 */
	int insertSelective(Stock record);

	/**
	 * This method was generated by MyBatis Generator. This method corresponds to the database table core.stock
	 * @mbg.generated  Fri Mar 23 15:56:38 EAT 2018
	 */
	List<Stock> selectByExample(StockExample example);

	/**
	 * This method was generated by MyBatis Generator. This method corresponds to the database table core.stock
	 * @mbg.generated  Fri Mar 23 15:56:38 EAT 2018
	 */
	Stock selectByPrimaryKey(Long id);

	/**
	 * This method was generated by MyBatis Generator. This method corresponds to the database table core.stock
	 * @mbg.generated  Fri Mar 23 15:56:38 EAT 2018
	 */
	int updateByExampleSelective(@Param("record") Stock record, @Param("example") StockExample example);

	/**
	 * This method was generated by MyBatis Generator. This method corresponds to the database table core.stock
	 * @mbg.generated  Fri Mar 23 15:56:38 EAT 2018
	 */
	int updateByExample(@Param("record") Stock record, @Param("example") StockExample example);

	/**
	 * This method was generated by MyBatis Generator. This method corresponds to the database table core.stock
	 * @mbg.generated  Fri Mar 23 15:56:38 EAT 2018
	 */
	int updateByPrimaryKeySelective(Stock record);

	/**
	 * This method was generated by MyBatis Generator. This method corresponds to the database table core.stock
	 * @mbg.generated  Fri Mar 23 15:56:38 EAT 2018
	 */
	int updateByPrimaryKey(Stock record);
}