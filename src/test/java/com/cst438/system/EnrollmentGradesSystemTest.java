package com.cst438.system;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EnrollmentGradesSystemTest {

    private static final String FRONTEND_URL = "http://localhost:5173/";

    private WebDriver driver;
    private WebDriverWait wait;

    @BeforeEach
    void setUp() {
        driver = new ChromeDriver();
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        driver.manage().window().maximize();
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    void instructorEntersEnrollmentGradesAndStudentViewsGradeOnTranscript() {
        loginAsInstructor();
        selectFall2025();
        openCst599Enrollments();

        clearExistingGrades();
        enterGrade("sama@csumb.edu", "A");
        enterGrade("samb@csumb.edu", "B+");
        enterGrade("samc@csumb.edu", "C");

        saveGrades();
        verifyGradesSavedMessage();

        reopenCst599Enrollments();
        assertGrade("sama@csumb.edu", "A");
        assertGrade("samb@csumb.edu", "B+");
        assertGrade("samc@csumb.edu", "C");

        logout();
        loginAsStudent();
        openTranscript();
        verifyTranscriptGrade("cst599", "B+");
    }

    private void loginAsInstructor() {
        login("ted@csumb.edu", "ted2025", "Instructor Home");
    }

    private void loginAsStudent() {
        login("samb@csumb.edu", "sam2025", "Student Home");
    }

    private void login(String email, String password, String homeHeading) {
        driver.get(FRONTEND_URL);

        WebElement emailInput = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("email"))
        );
        emailInput.sendKeys(email);

        driver.findElement(By.id("password")).sendKeys(password);
        driver.findElement(By.id("loginButton")).click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//h1[contains(normalize-space(), '"
                        + homeHeading + "')]")
        ));
    }

    private void selectFall2025() {
        WebElement getSectionsButton = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.xpath("//button[normalize-space()='Get Sections']")
                )
        );

        List<WebElement> termInputs = driver.findElements(By.xpath(
                "//button[normalize-space()='Get Sections']"
                        + "/preceding-sibling::input"
        ));

        assertEquals(
                2,
                termInputs.size(),
                "Expected year and semester input fields"
        );

        WebElement yearInput = termInputs.get(0);
        WebElement semesterInput = termInputs.get(1);

        yearInput.clear();
        yearInput.sendKeys("2025");

        semesterInput.clear();
        semesterInput.sendKeys("Fall");

        getSectionsButton.click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                cst599SectionRow()
        ));
    }

    private void openCst599Enrollments() {
        WebElement enrollmentsLink = wait.until(
                ExpectedConditions.elementToBeClickable(By.xpath(
                        "//tr[td[normalize-space()='cst599']]"
                                + "//a[normalize-space()='Enrollments']"
                ))
        );

        enrollmentsLink.click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(
                "//h3[contains(normalize-space(), 'cst599-1 Enrollments')]"
        )));
        wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(
                By.xpath("//input[@name='grade']")
        ));
    }

    private void clearExistingGrades() {
        List<WebElement> gradeInputs = wait.until(
                ExpectedConditions.visibilityOfAllElementsLocatedBy(
                        By.xpath("//input[@name='grade']")
                )
        );

        for (WebElement gradeInput : gradeInputs) {
            gradeInput.clear();
        }
    }

    private void enterGrade(String studentEmail, String grade) {
        WebElement gradeInput = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        gradeInput(studentEmail)
                )
        );

        gradeInput.clear();
        gradeInput.sendKeys(grade);
    }

    private void saveGrades() {
        wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[normalize-space()='Save']")
        )).click();
    }

    private void verifyGradesSavedMessage() {
        WebElement successMessage = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//*[normalize-space()='Grades saved']")
                )
        );

        assertEquals("Grades saved", successMessage.getText());
    }

    private void reopenCst599Enrollments() {
        wait.until(ExpectedConditions.elementToBeClickable(
                By.id("homeLink")
        )).click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//h1[contains(normalize-space(), 'Instructor Home')]")
        ));

        selectFall2025();
        openCst599Enrollments();
    }

    private void assertGrade(String studentEmail, String expectedGrade) {
        WebElement gradeInput = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        gradeInput(studentEmail)
                )
        );

        assertEquals(
                expectedGrade,
                gradeInput.getAttribute("value"),
                "Incorrect saved grade for " + studentEmail
        );
    }

    private void logout() {
        wait.until(ExpectedConditions.elementToBeClickable(
                By.id("logoutLink")
        )).click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("email")
        ));
    }

    private void openTranscript() {
        wait.until(ExpectedConditions.elementToBeClickable(
                By.id("transcriptLink")
        )).click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//h3[normalize-space()='Transcript']")
        ));
    }

    private void verifyTranscriptGrade(
            String courseId,
            String expectedGrade) {
        By courseRow = By.xpath(
                "//tr[td[normalize-space()='" + courseId + "']]"
        );

        wait.until(ExpectedConditions.textToBePresentInElementLocated(
                courseRow,
                expectedGrade
        ));

        WebElement transcriptRow = driver.findElement(courseRow);

        assertTrue(
                !transcriptRow.findElements(By.xpath(
                        "./td[normalize-space()='" + expectedGrade + "']"
                )).isEmpty(),
                "Expected " + courseId + " transcript grade to be "
                        + expectedGrade
        );
    }

    private By cst599SectionRow() {
        return By.xpath("//tr[td[normalize-space()='cst599']]");
    }

    private By gradeInput(String studentEmail) {
        return By.xpath(
                "//tr[td[normalize-space()='" + studentEmail + "']]"
                        + "//input[@name='grade']"
        );
    }
}
