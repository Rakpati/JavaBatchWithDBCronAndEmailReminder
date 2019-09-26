package com.batch.job;

import java.util.ArrayList;
import java.util.List;

import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

public class CronTrigger
{
	static String cronQuery = "select cronExpression from cronTimer where cronType = 'java'";
	public static void main( String[] args ) throws Exception
	{
		List<HdbBean> hdbBeans = new ArrayList<HdbBean>();
		Tasklet tasklet = new Tasklet();
		hdbBeans = tasklet.testDataSource(cronQuery, "cron",0,0);
		
		JobDetail job = JobBuilder.newJob(Tasklet.class)
				.withIdentity("dummyJobName", "group1").build();

		Trigger trigger = TriggerBuilder
				.newTrigger()
				.withIdentity("dummyTriggerName", "group1")
				.withSchedule(
						CronScheduleBuilder.cronSchedule(hdbBeans.get(0).getCronExpression()))
				.build();

		Scheduler scheduler = new StdSchedulerFactory().getScheduler();
		scheduler.start();
		scheduler.scheduleJob(job, trigger);
	}
}