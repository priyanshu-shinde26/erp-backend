package com.erp.erpbackend;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FirebaseTestController {

    @GetMapping("/api/test/firebase")
    public ResponseEntity<String> testFirebase() {
        try {
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("healthCheck");
            ref.setValueAsync("Backend connected ✅");
            return ResponseEntity.ok("Firebase connection success! Check DB for 'healthCheck' node.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Firebase error: " + e.getMessage());
        }
    }
}
