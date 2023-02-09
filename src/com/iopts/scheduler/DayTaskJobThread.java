package com.iopts.scheduler;

import java.util.UUID;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibatis.sqlmap.client.SqlMapClient;
import com.iopts.shell.ShellExecute;
import com.iopts.shell.ShellExecuteVo;
import com.skyun.app.util.config.AppConfig;
import com.skyun.recon.util.database.ibatis.SqlMapInstance;

public class DayTaskJobThread implements Job {
	private static Logger logger = LoggerFactory.getLogger(DayTaskJobThread.class);
	private static SqlMapClient sqlMap = null;
	
	@Override
	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		// TODO Auto-generated method stub
		
		this.sqlMap = SqlMapInstance.getSqlMapInstance();
		
		logger.info(arg0.getJobDetail().getKey().getName().toString());
		
		if(arg0.getJobDetail().getKey().getName().toString().contains("reconjobs")) {
			logger.info("reconjobs _________________________");
			new Thread(new ReconJobs()).start();	
		}else if(arg0.getJobDetail().getKey().getName().toString().contains("schedulerjob_day")) {
			logger.info("schedulerjob_day update _________________________");
			new Thread(new ReconSchedulerJobs()).start();	
		}else if(arg0.getJobDetail().getKey().getName().toString().contains("profilejobsjob_day")) {
			logger.info("profilejobs_day update_________________________");
			new Thread(new ReconProfileJob()).start();	
		}else if(arg0.getJobDetail().getKey().getName().toString().contains("insajobsjob_day")) {
			logger.info("profilejobs_day update_________________________");
			new Thread(new Insajob()).start();	
		}else if(arg0.getJobDetail().getKey().getName().toString().contains("monthlyjob")) {
			logger.info("profilejobs_day update_________________________");
			new Thread(new MonthlyJob()).start();	
		}else if(arg0.getJobDetail().getKey().getName().toString().contains("scheduleCheck")) {
			logger.info("scheduleCheck_day update_________________________");
			new Thread(new ScheduleCheck()).start();	
		}else if(arg0.getJobDetail().getKey().getName().toString().contains("shellbatch")) {
			logger.info("shellbatch call _________________________");
			
			ShellExecuteVo obj=new  ShellExecuteVo();
			obj.setAccount("root");
			obj.setShellid(UUID.randomUUID().toString());
			obj.setShell(new StringBuffer(AppConfig.getProperty("config.recon.schedule.runshell")));
			obj.setTimeout(30*1000);
			
			ShellExecuteVo retobj = (ShellExecuteVo) new ShellExecute((ShellExecuteVo) obj).getObject();
			
			logger.info("Executed getStderr:"+retobj.getStderr());
			logger.info("Executed exitcvoe:"+retobj.getExitcode());
		}
		
	}
	

}
