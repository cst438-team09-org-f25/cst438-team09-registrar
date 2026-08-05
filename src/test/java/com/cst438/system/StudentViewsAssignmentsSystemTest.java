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
import java.util.Random;

import org.openqa.selenium.JavascriptExecutor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StudentViewsAssignmentsSystemTest {

    private WebDriver driver;
    private WebDriverWait wait;

    private static final String FRONTEND_URL = "http://localhost:5173/";

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
    void studentViewsNewAssignmentWithBlankScore() {

        /*
         * Generate a unique assignment title so the test can be run
         * repeatedly without conflicting with an existing assignment.
         */
        String assignmentTitle =
                "assignment" + new Random().nextInt(1_000_000);

        /*
         * The due date must fall within the Fall 2025 term dates.
         */
        String dueDate = "2025-10-15";

        loginAsInstructor();

        selectFall2025();

        openCst599Assignments();

        addAssignment(assignmentTitle, dueDate);

        verifyAssignmentAppears(assignmentTitle);

        // TODO: Log out and log in as samb@csumb.edu.
        // TODO: Open the student assignments page and select Fall 2025.
        // TODO: Verify CST599 and the new assignment appear.
        // TODO: Verify the assignment score is blank.
    }

    private void loginAsInstructor() {
        driver.get(FRONTEND_URL);

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("email")
        ));

        driver.findElement(By.id("email"))
                .sendKeys("ted@csumb.edu");

        driver.findElement(By.id("password"))
                .sendKeys("ted2025");

        driver.findElement(By.id("loginButton")).click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//h1[contains(text(), 'Instructor Home')]")
        ));
    }

    private void selectFall2025() {

        /*
         * Locate the two term inputs associated with the
         * "Get Sections" button.
         *
         * The first input is year and the second is semester.
         */
        WebElement getSectionsButton =
                wait.until(ExpectedConditions.elementToBeClickable(
                        By.xpath("//button[normalize-space()='Get Sections']")
                ));

        List<WebElement> termInputs =
                driver.findElements(By.xpath(
                        "//button[normalize-space()='Get Sections']" +
                                "/preceding-sibling::input"
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
                By.xpath("//tr[td[normalize-space()='cst599']]")
        ));
    }

    private void openCst599Assignments() {

        WebElement assignmentsLink =
                wait.until(ExpectedConditions.elementToBeClickable(
                        By.xpath(
                                "//tr[td[normalize-space()='cst599']]" +
                                        "//a[normalize-space()='Assignments']"
                        )
                ));

        assignmentsLink.click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath(
                        "//h3[contains(normalize-space(), " +
                                "'Assignments for cst599')]"
                )
        ));
    }

    private void addAssignment(
            String assignmentTitle,
            String dueDate) {

        driver.findElement(By.id("addAssignmentButton")).click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("assignmentTitle")
        ));

        driver.findElement(By.id("assignmentTitle"))
                .sendKeys(assignmentTitle);

        WebElement dueDateInput =
                driver.findElement(By.id("assignmentDueDate"));

        JavascriptExecutor javascript = (JavascriptExecutor) driver;

        javascript.executeScript(
                """
                const input = arguments[0];
                const value = arguments[1];
                const setter =
                    Object.getOwnPropertyDescriptor(
                        HTMLInputElement.prototype,
                        'value'
                    ).set;
        
                setter.call(input, value);
                input.dispatchEvent(
                    new Event('input', { bubbles: true })
                );
                input.dispatchEvent(
                    new Event('change', { bubbles: true })
                );
                """,
                dueDateInput,
                dueDate
        );

        driver.findElement(By.id("saveAssignmentButton")).click();

        /*
         * AssignmentAdd closes its dialog after a successful POST.
         */
        wait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.id("assignmentTitle")
        ));
    }

    private void verifyAssignmentAppears(String assignmentTitle) {

        WebElement assignmentRow =
                wait.until(ExpectedConditions.visibilityOfElementLocated(
                        assignmentRow(assignmentTitle)
                ));

        assertTrue(
                assignmentRow.getText().contains(assignmentTitle),
                "New assignment title was not displayed"
        );
    }

    private By assignmentRow(String assignmentTitle) {
        return By.xpath(
                "//tr[td[normalize-space()='" +
                        assignmentTitle +
                        "']]"
        );
    }
}
