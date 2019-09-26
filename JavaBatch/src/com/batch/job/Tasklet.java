package com.batch.job;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;

import javax.sql.DataSource;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class Tasklet implements Job
{
	private static final String countQuery="select count(*) as count from Hdbstatus";
	private static final String selectQuery="select * from Hdbstatus order by id LIMIT ?, ?";
	private static final String updateQuery="update Hdbstatus set status = 'lapse' where id = ?";
	private List<HdbBean> hdbBeans;
	Connection con = null;
	PreparedStatement stmt = null;
	ResultSet rs = null;
	DataSource ds = null;

	public List<HdbBean> testDataSource(String query, String table, int lowerLimit, int upperLimit) {
		List<HdbBean> beanList = new ArrayList<HdbBean>();
		ds = MyDataSourceFactory.getMySQLDataSource();

		try {
			con = ds.getConnection();
			stmt = con.prepareStatement(query);
			if(upperLimit!=0) {
				stmt.setInt(1, lowerLimit);
				stmt.setInt(2, upperLimit);
				rs = stmt.executeQuery();
			}else {
				rs = stmt.executeQuery(query);
			}
			while(rs.next()){
				HdbBean hdbBean = new HdbBean();
				if(table.equalsIgnoreCase("cron")) {
					hdbBean.setCronExpression(rs.getString("cronexpression"));

				}else if(table.equalsIgnoreCase("hdbstatus")) {
					hdbBean.setId(rs.getInt("id"));
					hdbBean.setName(rs.getString("name"));
					hdbBean.setEmailId(rs.getString("email"));
					hdbBean.setStatusDate(rs.getDate("statusdate"));
					hdbBean.setStatus(rs.getString("status"));

				}else if(table.equalsIgnoreCase("recordCount")) {
					hdbBean.setRecordCount(rs.getInt("count"));

				}
				beanList.add(hdbBean);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}finally{
			try {
				if(rs != null) rs.close();
				if(stmt != null) stmt.close();
				if(con != null) con.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return beanList;
	}
	public void execute(JobExecutionContext context)
			throws JobExecutionException {
		try {
			hdbBeans = new ArrayList<HdbBean>();
			int recordCount = 0, lowerLimit = 0, upperLimit = 50;
			hdbBeans = testDataSource(countQuery, "recordCount",0,0);
			recordCount = hdbBeans.get(0).getRecordCount();
			System.out.println(recordCount);
			hdbBeans = new ArrayList<HdbBean>();
			while(lowerLimit <= recordCount) {
				hdbBeans = testDataSource(selectQuery, "hdbstatus",lowerLimit,upperLimit);
				for(HdbBean bean : hdbBeans) {
					checkDate(bean);
					lowerLimit = lowerLimit + upperLimit;
				}
				System.out.println("Completed");	
			}
		}catch (SQLException e) {
			e.printStackTrace();
		}
	}
	private void checkDate(HdbBean hdbStatus) throws SQLException {
		LocalDate localDate = LocalDate.now();
		final TreeSet<String> name = new TreeSet<String>();
		final TreeSet<String> email = new TreeSet<String>();
		final TreeSet<Integer> days = new TreeSet<Integer>();

		Date date1 = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
		System.out.println("Date      = " + date1);
		System.out.println(date1.compareTo(hdbStatus.getStatusDate()));

		long difference = date1.getTime() - hdbStatus.getStatusDate().getTime();
		int daysBetween = (int) (difference / (1000*60*60*24));
		System.out.println(daysBetween);


		if(hdbStatus.getStatus().equalsIgnoreCase("due") && daysBetween > 2) {
			SendMail mm = new SendMail(); 
			name.add(hdbStatus.getName());
			email.add(hdbStatus.getEmailId()); 
			days.add(daysBetween);

			if(daysBetween == 3 || daysBetween == 7){
				mm.mailSender(name, email, days);
			}else if(daysBetween == 21) { 
				mm.mailSender(name, email, days);
				updateRecord(hdbStatus.getId()); 
			}
		}

	}
	private void updateRecord(int id) throws SQLException {
		con = ds.getConnection();
		stmt = con.prepareStatement(updateQuery);
		stmt.setInt(1, id);
		stmt.executeUpdate();
		System.out.println("Records Updated");
	}
}