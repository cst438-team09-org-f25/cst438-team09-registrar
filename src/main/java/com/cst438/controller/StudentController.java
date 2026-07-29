package com.cst438.controller;

import com.cst438.domain.*;
import com.cst438.dto.EnrollmentDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

@RestController
public class StudentController {

   private final EnrollmentRepository enrollmentRepository;
   private final UserRepository userRepository;

   public StudentController(
           EnrollmentRepository enrollmentRepository,
           UserRepository userRepository
   ) {
       this.enrollmentRepository = enrollmentRepository;
       this.userRepository = userRepository;
   }

   // retrieve schedule for student for a term
   @GetMapping("/enrollments")
   @PreAuthorize("hasAuthority('SCOPE_ROLE_STUDENT')")
   public List<EnrollmentDTO> getSchedule(
           @RequestParam("year") int year,
           @RequestParam("semester") String semester,
           Principal principal) {
			   
		// use the EnrollmentController findByYearAndSemsterOrderByCourseId
		// method to retrive the enrollments given the year, semester and id 
		// of the logged in student.
		// Return a list of EnrollmentDTO.
       User student = getLoggedInStudent(principal);

       List<Enrollment> enrollments =
               enrollmentRepository.findByYearAndSemesterOrderByCourseId(
                       year,
                       semester,
                       student.getId()
               );

       List<EnrollmentDTO> result = new ArrayList<>();

       for (Enrollment enrollment : enrollments) {
           result.add(toDTO(enrollment));
       }

       return result;
   }

   // return transcript for student
    @GetMapping("/transcripts")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_STUDENT')")
    public List<EnrollmentDTO> getTranscript(Principal principal) {

        // use the EnrollmentController findEnrollmentsByStudentIdOrderByTermId
		// method to retrive the enrollments given the id 
		// of the logged in student.
		// Return a list of EnrollmentDTO.

        User student = getLoggedInStudent(principal);

        List<Enrollment> enrollments =
                enrollmentRepository.findEnrollmentsByStudentIdOrderByTermId(
                        student.getId()
                );

        List<EnrollmentDTO> result = new ArrayList<>();

        for (Enrollment enrollment : enrollments) {
            result.add(toDTO(enrollment));
        }

        return result;
    }

    private User getLoggedInStudent(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "student is not authenticated"
            );
        }

        User student = userRepository.findByEmail(principal.getName());

        if (student == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "student not found"
            );
        }

        return student;
    }

    private EnrollmentDTO toDTO(Enrollment enrollment) {
        User student = enrollment.getStudent();
        Section section = enrollment.getSection();

        return new EnrollmentDTO(
                enrollment.getEnrollmentId(),
                enrollment.getGrade(),
                student.getId(),
                student.getName(),
                student.getEmail(),
                section.getCourse().getCourseId(),
                section.getCourse().getTitle(),
                section.getSectionId(),
                section.getSectionNo(),
                section.getBuilding(),
                section.getRoom(),
                section.getTimes(),
                section.getCourse().getCredits(),
                section.getTerm().getYear(),
                section.getTerm().getSemester()
        );
    }
}