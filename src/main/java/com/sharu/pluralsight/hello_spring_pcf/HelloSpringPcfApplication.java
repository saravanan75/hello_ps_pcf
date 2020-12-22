package com.sharu.pluralsight.hello_spring_pcf;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.cloud.app.ApplicationInstanceInfo;


import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

@SpringBootApplication
@Controller
public class HelloSpringPcfApplication {

	@Autowired(required = false) RedisConnectionFactory redisConnectionFactory;
	@Autowired(required = false) ConnectionFactory rabbitConnectionFactory;
	@Autowired(required = false) ApplicationInstanceInfo instanceInfo;

	@Autowired
	DataSource dataSource;

	@RequestMapping("/")
	public String home(Model model) {
		model.addAttribute("instanceInfo", instanceInfo);

		if (instanceInfo != null) {
			Map<Class<?>, String> services = new LinkedHashMap<Class<?>, String>();
			services.put(redisConnectionFactory.getClass(), toString(redisConnectionFactory));
			services.put(rabbitConnectionFactory.getClass(), toString(rabbitConnectionFactory));
			model.addAttribute("services", services.entrySet());
		}

		return "home";
	}

	private String toString(RedisConnectionFactory redisConnectionFactory) {
		if (redisConnectionFactory == null) {
			return "<none>";
		} else {
			if (redisConnectionFactory instanceof JedisConnectionFactory) {
				JedisConnectionFactory jcf = (JedisConnectionFactory) redisConnectionFactory;
				return jcf.getHostName().toString() + ":" + jcf.getPort();
			} else if (redisConnectionFactory instanceof LettuceConnectionFactory) {
				LettuceConnectionFactory lcf = (LettuceConnectionFactory) redisConnectionFactory;
				return lcf.getHostName().toString() + ":" + lcf.getPort();
			}
			return "<unknown> " + redisConnectionFactory.getClass();
		}
	}

	private String toString(ConnectionFactory rabbitConnectionFactory) {
		if (rabbitConnectionFactory == null) {
			return "<none>";
		} else {
			return rabbitConnectionFactory.getHost() + ":"
					+ rabbitConnectionFactory.getPort();
		}
	}

	@RequestMapping("/db")
	public String query(Map<String, Object> model){
		try (Connection connection = dataSource.getConnection()) {
			Statement stmt = connection.createStatement();
			stmt.executeUpdate("CREATE TABLE IF NOT EXISTS ticks (tick timestamp)");
			stmt.executeUpdate("INSERT INTO ticks VALUES (now())");
			ResultSet rs = stmt.executeQuery("SELECT tick FROM ticks");

			ArrayList<String> output = new ArrayList<String>();
			while (rs.next()) {
				output.add("Read from DB: " + rs.getTimestamp("tick"));
			}

			model.put("records", output);
			return "db";
		} catch (Exception e) {
			model.put("message", e.getMessage());
			return "error";
		}
	}

	@RequestMapping("/hellops")
	@ResponseBody
	public String helloPCF()
	{
		System.out.println("About to print the welcome message");
		return "Hello from pluralsight course";
	}

	public static void main(String[] args) {

		SpringApplication.run(HelloSpringPcfApplication.class, args);
	}

}
