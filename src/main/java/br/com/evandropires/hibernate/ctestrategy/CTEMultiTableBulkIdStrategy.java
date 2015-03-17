package br.com.evandropires.hibernate.ctestrategy;

import java.util.Map;

import org.hibernate.cfg.Mappings;
import org.hibernate.engine.jdbc.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.hql.internal.ast.HqlSqlWalker;
import org.hibernate.hql.spi.MultiTableBulkIdStrategy;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.persister.entity.Queryable;
import org.jboss.logging.Logger;

public class CTEMultiTableBulkIdStrategy implements MultiTableBulkIdStrategy {

	public static final CTEMultiTableBulkIdStrategy INSTANCE = new CTEMultiTableBulkIdStrategy();

	public static final String SHORT_NAME = "temporary";

	private static final CoreMessageLogger log = Logger.getMessageLogger(
			CoreMessageLogger.class,
			CTEMultiTableBulkIdStrategy.class.getName());

	@Override
	public void prepare(JdbcServices jdbcServices,
			JdbcConnectionAccess connectionAccess, Mappings arg2, Mapping arg3,
			Map arg4) {
		// nothing to do
	}

	@Override
	public void release(JdbcServices jdbcServices,
			JdbcConnectionAccess connectionAccess) {
		// nothing to do
	}

	@Override
	public UpdateHandler buildUpdateHandler(SessionFactoryImplementor factory,
			HqlSqlWalker walker) {
		return new CTEBasedUpdateHandlerImpl(factory, walker) {
			@Override
			protected void prepareForUse(Queryable persister,
					SessionImplementor session) {
			}

			@Override
			protected void releaseFromUse(Queryable persister,
					SessionImplementor session) {
			}
		};
	}

	@Override
	public DeleteHandler buildDeleteHandler(SessionFactoryImplementor factory,
			HqlSqlWalker walker) {
		return new CTEBasedDeleteHandlerImpl(factory, walker) {
			@Override
			protected void prepareForUse(Queryable persister,
					SessionImplementor session) {
			}

			@Override
			protected void releaseFromUse(Queryable persister,
					SessionImplementor session) {
			}
		};
	}

	protected boolean shouldIsolateTemporaryTableDDL(SessionImplementor session) {
		Boolean dialectVote = session.getFactory().getDialect()
				.performTemporaryTableDDLInIsolation();
		if (dialectVote != null) {
			return dialectVote;
		}
		return session.getFactory().getSettings()
				.isDataDefinitionImplicitCommit();
	}

}
