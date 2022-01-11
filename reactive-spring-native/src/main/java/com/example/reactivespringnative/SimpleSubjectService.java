package com.example.reactivespringnative;

class SimpleSubjectService implements SubjectService{

  

    public SimpleSubjectService() {
        super();
    }

    @Override
    public Subject findFirstHiddenSubject() {
       
        return  new Subject(720, "Hidden Subject - Keep hidden", "Niow Blety");
    }
    
    
}
