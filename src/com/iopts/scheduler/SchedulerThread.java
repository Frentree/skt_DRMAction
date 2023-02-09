package com.iopts.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SchedulerThread implements Runnable{
	private static Logger logger = LoggerFactory.getLogger(CronTriggerSchedulerInstance.class);
	private static CronTriggerSchedulerInstance instance=null;
	
	
	public SchedulerThread() {
		this.instance=CronTriggerSchedulerInstance.getSchdInstance();
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		try {
			this.instance.SchedulerRun();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
