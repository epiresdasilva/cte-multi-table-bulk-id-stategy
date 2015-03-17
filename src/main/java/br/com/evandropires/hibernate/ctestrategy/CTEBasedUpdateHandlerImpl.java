package br.com.evandropires.hibernate.ctestrategy;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.hql.internal.ast.HqlSqlWalker;
import org.hibernate.hql.internal.ast.tree.AssignmentSpecification;
import org.hibernate.hql.internal.ast.tree.FromElement;
import org.hibernate.hql.internal.ast.tree.UpdateStatement;
import org.hibernate.hql.spi.MultiTableBulkIdStrategy;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.param.ParameterSpecification;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.sql.Update;
import org.jboss.logging.Logger;

public class CTEBasedUpdateHandlerImpl extends AbstractCTEBasedBulkIdHandler
		implements MultiTableBulkIdStrategy.UpdateHandler {

	private static final Logger log = Logger
			.getLogger(CTEBasedUpdateHandlerImpl.class);

	private final Queryable targetedPersister;

	private final String idCteSelect;
	private final List<ParameterSpecification> idSelectParameterSpecifications;

	private final String[] updates;
	private final ParameterSpecification[][] assignmentParameterSpecifications;

	public CTEBasedUpdateHandlerImpl(SessionFactoryImplementor factory,
			HqlSqlWalker walker) {
		this(factory, walker, null, null);
	}

	public CTEBasedUpdateHandlerImpl(SessionFactoryImplementor factory,
			HqlSqlWalker walker, String catalog, String schema) {
		super(factory, walker, catalog, schema);

		UpdateStatement updateStatement = (UpdateStatement) walker.getAST();
		FromElement fromElement = updateStatement.getFromClause()
				.getFromElement();

		this.targetedPersister = fromElement.getQueryable();

		final ProcessedWhereClause processedWhereClause = processWhereClause(updateStatement
				.getWhereClause());
		this.idSelectParameterSpecifications = processedWhereClause
				.getIdSelectParameterSpecifications();

		this.idCteSelect = generateIdCteSelect(targetedPersister,
				determineIdTableName(targetedPersister), processedWhereClause);

		log.tracev("Generated ID-CTE-SELECT SQL (multi-table update) : {0}",
				idCteSelect);

		String[] tableNames = targetedPersister
				.getConstraintOrderedTableNameClosure();
		String[][] columnNames = targetedPersister
				.getContraintOrderedTableKeyColumnClosure();
		String idSubselect = generateIdSubselect(targetedPersister);

		updates = new String[tableNames.length];
		assignmentParameterSpecifications = new ParameterSpecification[tableNames.length][];
		for (int tableIndex = 0; tableIndex < tableNames.length; tableIndex++) {
			boolean affected = false;
			final List<ParameterSpecification> parameterList = new ArrayList<ParameterSpecification>();
			final Update update = new Update(factory().getDialect())
					.setTableName(tableNames[tableIndex]).setWhere(
							"("
									+ StringHelper.join(", ",
											columnNames[tableIndex]) + ") IN ("
									+ idSubselect + ")");
			if (factory().getSettings().isCommentsEnabled()) {
				update.setComment("bulk update");
			}
			final List<AssignmentSpecification> assignmentSpecifications = walker
					.getAssignmentSpecifications();
			for (AssignmentSpecification assignmentSpecification : assignmentSpecifications) {
				if (assignmentSpecification
						.affectsTable(tableNames[tableIndex])) {
					affected = true;
					update.appendAssignmentFragment(assignmentSpecification
							.getSqlAssignmentFragment());
					if (assignmentSpecification.getParameters() != null) {
						for (int paramIndex = 0; paramIndex < assignmentSpecification
								.getParameters().length; paramIndex++) {
							parameterList.add(assignmentSpecification
									.getParameters()[paramIndex]);
						}
					}
				}
			}
			if (affected) {
				updates[tableIndex] = update.toStatementString();
				assignmentParameterSpecifications[tableIndex] = parameterList
						.toArray(new ParameterSpecification[parameterList
								.size()]);
			}
		}
	}

	@Override
	public Queryable getTargetedQueryable() {
		return targetedPersister;
	}

	@Override
	public String[] getSqlStatements() {
		return updates;
	}

	@Override
	public int execute(SessionImplementor session,
			QueryParameters queryParameters) {
		prepareForUse(targetedPersister, session);
		try {
			// First, save off the pertinent ids, as the return value
			PreparedStatement ps = null;
			int resultCount = 0;
			// Start performing the updates
			for (int i = 0; i < updates.length; i++) {
				if (updates[i] == null) {
					continue;
				}
				try {
					try {
						String update = updates[i];
						update = idCteSelect + update;

						ps = session.getTransactionCoordinator()
								.getJdbcCoordinator().getStatementPreparer()
								.prepareStatement(update, false);
						int position = 1; // jdbc params are 1-based
						position += handlePrependedParametersOnIdSelection(ps,
								session, position);
						for (ParameterSpecification parameterSpecification : idSelectParameterSpecifications) {
							position += parameterSpecification.bind(ps,
									queryParameters, session, position);
						}
						if (assignmentParameterSpecifications[i] != null) {
							for (int x = 0; x < assignmentParameterSpecifications[i].length; x++) {
								position += assignmentParameterSpecifications[i][x]
										.bind(ps, queryParameters, session,
												position);
							}
							handleAddedParametersOnUpdate(ps, session, position);
						}
						resultCount = session.getTransactionCoordinator()
								.getJdbcCoordinator().getResultSetReturn()
								.executeUpdate(ps);
					} finally {
						if (ps != null) {
							session.getTransactionCoordinator()
									.getJdbcCoordinator().release(ps);
						}
					}
				} catch (SQLException e) {
					throw convert(e, "error performing bulk update", updates[i]);
				}
			}

			return resultCount;
		} finally {
			releaseFromUse(targetedPersister, session);
		}
	}

	protected int handlePrependedParametersOnIdSelection(PreparedStatement ps,
			SessionImplementor session, int pos) throws SQLException {
		return 0;
	}

	protected void handleAddedParametersOnUpdate(PreparedStatement ps,
			SessionImplementor session, int position) throws SQLException {
	}
}
