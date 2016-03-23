package br.com.evandropires.hibernate.ctestrategy;

import java.util.LinkedList;
import java.util.List;

public class CTEValues {

	private String idCteSelect;
	private List<Object[]> selectResult = new LinkedList<Object[]>();

	public CTEValues() {
	}

	public CTEValues(String idCteSelect, List<Object[]> selectResult) {
		this.idCteSelect = idCteSelect;
		this.selectResult = selectResult;
	}

	public String getIdCteSelect() {
		return idCteSelect;
	}

	public void setIdCteSelect(String idCteSelect) {
		this.idCteSelect = idCteSelect;
	}

	public List<Object[]> getSelectResult() {
		return selectResult;
	}

	public void setSelectResult(List<Object[]> selectResult) {
		this.selectResult = selectResult;
	}

}
