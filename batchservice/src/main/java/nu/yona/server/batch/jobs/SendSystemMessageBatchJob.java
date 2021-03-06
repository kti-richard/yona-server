/*******************************************************************************
 * Copyright (c) 2017 Stichting Yona Foundation This Source Code Form is subject to the terms of the Mozilla Public License, v.
 * 2.0. If a copy of the MPL was not distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *******************************************************************************/
package nu.yona.server.batch.jobs;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import nu.yona.server.exceptions.YonaException;
import nu.yona.server.messaging.entities.MessageDestination;
import nu.yona.server.messaging.entities.MessageDestinationRepository;
import nu.yona.server.messaging.entities.SystemMessage;
import nu.yona.server.subscriptions.service.UserAnonymizedDto;
import nu.yona.server.subscriptions.service.UserAnonymizedService;

@Component
public class SendSystemMessageBatchJob
{
	private static final Logger logger = LoggerFactory.getLogger(ActivityAggregationBatchJob.class);

	private static final int USERS_CHUNK_SIZE = 50;

	@Autowired
	private JobBuilderFactory jobBuilderFactory;

	@Autowired
	private StepBuilderFactory stepBuilderFactory;

	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	@Qualifier("sendSystemMessageJobUserAnonymizedReader")
	private ItemReader<UUID> userAnonymizedReader;

	@Autowired
	@Qualifier("sendSystemMessageJobUserAnonymizedProcessor")
	private ItemProcessor<UUID, MessageDestination> userAnonymizedProcessor;

	@Autowired
	private DataSource dataSource;

	@Autowired
	private UserAnonymizedService userAnonymizedService;

	@Autowired
	private MessageDestinationRepository messageDestinationRepository;

	@Bean("sendSystemMessageJob")
	public Job sendSystemMessagesBatchJob()
	{
		return jobBuilderFactory.get("sendSystemMessagesBatchJob").incrementer(new RunIdIncrementer()).flow(sendSystemMessages())
				.end().build();
	}

	private Step sendSystemMessages()
	{
		return stepBuilderFactory.get("sendSystemMessages").<UUID, MessageDestination> chunk(USERS_CHUNK_SIZE)
				.reader(userAnonymizedReader).processor(userAnonymizedProcessor).writer(messageDestinationWriter()).build();
	}

	@Bean(name = "sendSystemMessageJobUserAnonymizedReader", destroyMethod = "")
	@StepScope
	public ItemReader<UUID> userAnonymizedReader()
	{
		return userAnonymizedIdReader();
	}

	@Bean(name = "sendSystemMessageJobUserAnonymizedProcessor", destroyMethod = "")
	@StepScope
	private ItemProcessor<UUID, MessageDestination> userAnonymizedProcessor()
	{
		return new ItemProcessor<UUID, MessageDestination>() {
			@Value("#{jobParameters['messageText']}")
			private String messageText;

			@Override
			public MessageDestination process(UUID userAnonymizedId) throws Exception
			{
				logger.debug("Processing user anonymized with id {}", userAnonymizedId);
				UserAnonymizedDto userAnonymized = userAnonymizedService.getUserAnonymized(userAnonymizedId);

				MessageDestination messageDestination = messageDestinationRepository
						.findOne(userAnonymized.getAnonymousDestination().getId());
				messageDestination.send(SystemMessage.createInstance(messageText));

				return messageDestination;
			}
		};
	}

	private JpaItemWriter<MessageDestination> messageDestinationWriter()
	{
		JpaItemWriter<MessageDestination> writer = new JpaItemWriter<>();
		writer.setEntityManagerFactory(entityManager.getEntityManagerFactory());

		return writer;
	}

	private ItemReader<UUID> userAnonymizedIdReader()
	{
		try
		{
			JdbcPagingItemReader<UUID> reader = new JdbcPagingItemReader<>();
			final SqlPagingQueryProviderFactoryBean sqlPagingQueryProviderFactoryBean = new SqlPagingQueryProviderFactoryBean();
			sqlPagingQueryProviderFactoryBean.setDataSource(dataSource);
			sqlPagingQueryProviderFactoryBean.setSelectClause("select id");
			sqlPagingQueryProviderFactoryBean.setFromClause("from users_anonymized");
			sqlPagingQueryProviderFactoryBean.setSortKey("id");
			reader.setQueryProvider(sqlPagingQueryProviderFactoryBean.getObject());
			reader.setDataSource(dataSource);
			reader.setPageSize(USERS_CHUNK_SIZE);
			reader.setRowMapper(singleUUIDColumnRowMapper());
			reader.afterPropertiesSet();
			reader.setSaveState(true);
			logger.info("Reading users anonymized in chunks of {}", USERS_CHUNK_SIZE);
			return reader;
		}
		catch (Exception e)
		{
			throw YonaException.unexpected(e);
		}
	}

	private RowMapper<UUID> singleUUIDColumnRowMapper()
	{
		return new RowMapper<UUID>() {

			@Override
			public UUID mapRow(ResultSet rs, int rowNum) throws SQLException
			{
				return UUID.fromString(rs.getString(1));
			}

		};
	}
}