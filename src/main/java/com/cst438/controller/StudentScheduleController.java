package com.cst438.controller;

import com.cst438.domain.*;
import com.cst438.dto.EnrollmentDTO;
import com.cst438.dto.SectionDTO;
import com.cst438.service.GradebookServiceProxy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Date;

@RestController
public class StudentScheduleController {

    private final EnrollmentRepository enrollmentRepository;
    private final SectionRepository sectionRepository;
    private final UserRepository userRepository;
    private final GradebookServiceProxy gradebook;

    public StudentScheduleController(
            EnrollmentRepository enrollmentRepository,
            SectionRepository sectionRepository,
            UserRepository userRepository,
            GradebookServiceProxy gradebook
    ) {
        this.enrollmentRepository = enrollmentRepository;
        this.sectionRepository = sectionRepository;
        this.userRepository = userRepository;
        this.gradebook = gradebook;
    }


    @PostMapping("/enrollments/sections/{sectionNo}")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_STUDENT')")
    public EnrollmentDTO addCourse(
            @PathVariable int sectionNo,
            Principal principal ) throws Exception  {

        // create and save an EnrollmentEntity
        //  relate enrollment to the student's User entity and to the Section entity
        //  check that student is not already enrolled in the section
        //  check that the current date is not before addDate, not after addDeadline
		//  of the section's term.  Return an EnrollmentDTO with the id of the 
		//  Enrollment and other fields.
        User student = getLoggedInStudent(principal);

        Section section = sectionRepository.findById(sectionNo)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "section not found"
                ));

        Enrollment existingEnrollment =
                enrollmentRepository.findEnrollmentBySectionNoAndStudentId(
                        sectionNo,
                        student.getId()
                );

        if (existingEnrollment != null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "student is already enrolled in this section"
            );
        }

        Term term = section.getTerm();
        Date today = new Date(System.currentTimeMillis());

        if (today.before(term.getAddDate())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "course cannot be added before the add date"
            );
        }

        if (today.after(term.getAddDeadline())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "add deadline has passed"
            );
        }

        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setSection(section);
        enrollment.setGrade(null);

        Enrollment savedEnrollment = enrollmentRepository.save(enrollment);

        return toDTO(savedEnrollment);
    }

    // student drops a course
    @DeleteMapping("/enrollments/{enrollmentId}")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_STUDENT')")
    public void dropCourse(@PathVariable("enrollmentId") int enrollmentId, Principal principal) throws Exception {

        // check that enrollment belongs to the logged in student
		// and that today is not after the dropDeadLine for the term.
        User student = getLoggedInStudent(principal);

        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "enrollment not found"
                ));

        if (enrollment.getStudent().getId() != student.getId()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "enrollment does not belong to the logged-in student"
            );
        }

        Date today = new Date(System.currentTimeMillis());
        Date dropDeadline = enrollment.getSection()
                .getTerm()
                .getDropDeadline();

        if (today.after(dropDeadline)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "drop deadline has passed"
            );
        }

        enrollmentRepository.delete(enrollment);
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
