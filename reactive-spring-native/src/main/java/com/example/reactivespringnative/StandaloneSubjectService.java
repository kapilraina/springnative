package com.example.reactivespringnative;

public class StandaloneSubjectService {

    public Subject findFirstHiddenSubject() {
       
        return  new Subject(720, "AOT Hidden Subject - Keep hidden", "Liop Xerces");
    }

    public Subject findTopHiddenSubject() {
       
        return  new Subject(220, "ML with Python", "Niue Vcer");
    }
    
    
}
