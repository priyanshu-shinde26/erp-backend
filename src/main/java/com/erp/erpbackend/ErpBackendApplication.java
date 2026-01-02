package com.erp.erpbackend;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.io.FileInputStream;
import java.io.IOException;

@SpringBootApplication
public class ErpBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(ErpBackendApplication.class, args);
	}


}
