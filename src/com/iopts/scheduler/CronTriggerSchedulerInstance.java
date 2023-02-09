package com.iopts.scheduler;

import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.skyun.app.util.config.AppConfig;


public class CronTriggerSchedulerInstance {
	private static Logger logger = LoggerFactory.getLogger(CronTriggerSchedulerInstance.class);

	public static Scheduler sched = null;

	private static CronTriggerSchedulerInstance instance = null;
	
	private static String customer_id = "";

	public CronTriggerSchedulerInstance() {
		try {
			sched = StdSchedulerFactory.getDefaultScheduler();
			sched.start();
		} catch (SchedulerException e) {
			e.printStackTrace();
		}
	}

	public void SchedulerRun() throws InterruptedException {
		while (true) {
			try {
				sched = StdSchedulerFactory.getDefaultScheduler();
				sched.start();
				
				customer_id = AppConfig.getProperty("config.customer");
				
				makeFixedJobTask("reconjobs");
				if(!"SKT".equals(customer_id)) {
					makeFixedJobTask("profilejobs");
				}
				makeFixedJobTask("shellbatch");
				if("SH".equals(customer_id)) {
					makeFixedJobTask("scheduler");		// scheduleCheck 생성으로 인한 신한은행만 사용_20210708
				} else if ("MERITZ".equals(customer_id)) {		// MERITZ는 인사 & 월간 보고서 작업 사용
					makeFixedJobTask("insajobs");
					makeFixedJobTask("monthlyjobs");
				} else {
					makeFixedJobTask("scheduleCheck");
				}
				
				
				Thread.sleep((((60 * 1000) * 60) * 24) * 365);
				
				sched.pauseAll();
				sched.clear();
				sched.shutdown(true);
				
			} catch (SchedulerException e) {
				logger.error("------- Job Already exist  -------------------");
			}
		}
	}

	public void makeFixedJobTask(String str) {
		
		String ScheduleString = AppConfig.getProperty("config.recon.schedule."+str);
		
		logger.info("makeFixedJobTask Scheduler :"+ScheduleString);

		String[] sString = ScheduleString.split(",");

		for (String source : sString) {
			String[] act = source.split("&");
			if (act.length == 2) {
				try {
					JobDetail job = JobBuilder.newJob(DayTaskJobThread.class).withIdentity(str+"job_" + act[1], str+"group_" + act[1]).withDescription(act[1]).build();
					
					CronTrigger trigger = (CronTrigger) TriggerBuilder.newTrigger().withIdentity(str+"trigger" + act[1], str+"group_" + act[1])
							.withSchedule(CronScheduleBuilder.cronSchedule(act[0])).build();
					
					JobDataMap jobDataMap = job.getJobDataMap();

					jobDataMap.put("KIND", str);
					sched.scheduleJob(job, trigger);
					
					logger.info("[OK] " + str + " >> " + ScheduleString);

				} catch (SchedulerException e) {
					e.printStackTrace();
					logger.error("[SchedulerException ERROR] >>" + ScheduleString + "::"+source);
				}
			} else {
				logger.error("[ERROR] >>" + ScheduleString + "::"+source);
			}
		}
		
	}

	public static CronTriggerSchedulerInstance getSchdInstance() {
		if (instance == null) {
			instance = new CronTriggerSchedulerInstance();
		}
		return instance;
	}
}
