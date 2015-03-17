package br.com.evandropires.hibernate.ctestrategy;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class CTE {

	private String tableName;
	private List<String> columns = new LinkedList<String>();
	private Integer numberOfParameters = 0;

	public CTE(String tableName) {
		this.tableName = tableName;
	}

	public CTE addColumn(String columnName) {
		columns.add(columnName);
		return this;
	}

	public CTE addColumns(String[] columns) {
		if (columns != null) {
			for (String column : columns) {
				addColumn(column);
			}
		}
		return this;
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public Integer getNumberOfParameters() {
		return numberOfParameters;
	}

	public void setNumberOfParameters(Integer numberOfParameters) {
		this.numberOfParameters = numberOfParameters;
	}

	public String toStatementString() {
		StringBuilder buf = new StringBuilder((columns.size() * 15)
				+ tableName.length() + 10);

		buf.append("with ").append(tableName).append(" ( ");
		Iterator<String> iterator = columns.iterator();
		while (iterator.hasNext()) {
			buf.append(iterator.next());
			if (iterator.hasNext()) {
				buf.append(", ");
			}
		}
		String parameters = "";
		for (int i = 0; i < this.numberOfParameters; i++) {
			if (!parameters.isEmpty()) {
				parameters += ", ";
			}
			parameters += "?";
		}
		buf.append(" ) as ( select unnest(array[").append(parameters)
				.append("]) )");
		return buf.toString();
	}

}
