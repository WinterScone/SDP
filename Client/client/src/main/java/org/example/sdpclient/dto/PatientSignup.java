package org.example.sdpclient.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PatientSignup {

    private String username;
    private String password;
    private String firstName;
    private String lastName;
    private String dateOfBirth;
    private String email;
    private String phone;
}

