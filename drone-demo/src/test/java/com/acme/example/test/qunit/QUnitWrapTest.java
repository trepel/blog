package com.acme.example.test.qunit;

import java.io.File;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;

import org.hamcrest.CoreMatchers;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.acme.example.test.ArchiveUtils;
import com.acme.example.test.Deployments;
import com.google.common.base.Function;

@RunWith(Arquillian.class)
@Ignore
public class QUnitWrapTest {

    private static final String QUNIT_SRC = "src/test/qunit";

    private static final long QUNIT_EXECUTION_TIMEOUT_IN_SECONDS = 10;

    @ArquillianResource
    URL contextPath;

    @Drone
    WebDriver driver;

    @Rule
    public ErrorCollector collector = new ErrorCollector();

    // list of urls to be tested
    private List<String> urls = Arrays.asList("qunit-tests1.html");

    @Deployment(testable = false)
    public static Archive<?> getApplicationDeployment() {
        WebArchive war = Deployments.createDeployment();
        // add qunit resources to the archive
        return ArchiveUtils.addWebResourcesRecursively(war, new File(QUNIT_SRC));
    }

    @Test
    public void qunitWrap() throws Exception {

        for (String url : urls) {

            int totalFailures = 0;
            int totalPasses = 0;

            driver.get(contextPath.toString() + url);

            checkElementPresence(driver, By.id("qunit-testresult"), MessageFormat.format(
                    "QUnit tests were not executed within {0} seconds.", QUNIT_EXECUTION_TIMEOUT_IN_SECONDS),
                    QUNIT_EXECUTION_TIMEOUT_IN_SECONDS);
            /*
             * Get the number of failed tests.
             */
            WebElement failed = driver.findElement(By.className("failed"));
            /*
             * Get the number of passed tests.
             */
            WebElement passed = driver.findElement(By.className("passed"));
            int failedTestCount = Integer.parseInt(failed.getText());
            int passedTestCount = Integer.parseInt(passed.getText());
            totalFailures = totalFailures + failedTestCount;
            totalPasses = totalPasses + passedTestCount;

            List<WebElement> failedTestCases = driver.findElement(By.id("qunit-tests")).findElements(
                    By.xpath("li[@class='fail']/strong"));
            List<WebElement> passedTestCases = driver.findElement(By.id("qunit-tests")).findElements(
                    By.xpath("li[@class='pass']/strong"));

            // Add the test case names to the failure list.
            for (WebElement failedTestCase : failedTestCases) {
                collector.checkThat(MessageFormat.format("QUnit test {0} failed", failedTestCase.getText()), false,
                        CoreMatchers.is(true));
            }
            // Add the test case names to the passed list.
            for (WebElement passedTestCase : passedTestCases) {
                collector.checkThat(MessageFormat.format("QUnit test {0} passed", passedTestCase.getText()), true,
                        CoreMatchers.is(true));
            }

        }

    }

    // check is element is presence on page, fails otherwise
    protected void checkElementPresence(final WebDriver driver, final By by, final String errorMsg, long timeoutInSeconds) {
        new WebDriverWaitWithMessage(driver, timeoutInSeconds).failWith(errorMsg).until(new ExpectedCondition<Boolean>() {
            @Override
            public Boolean apply(WebDriver webDriver) {
                try {
                    return driver.findElement(by).isDisplayed();
                } catch (NoSuchElementException ignored) {
                    return false;
                } catch (StaleElementReferenceException ignored) {
                    return false;
                }
            }
        });
    }

    protected static class WebDriverWaitWithMessage extends WebDriverWait {

        private String message;

        public WebDriverWaitWithMessage(WebDriver driver, long timeOutInSeconds) {
            super(driver, timeOutInSeconds);
        }

        public WebDriverWait failWith(String message) {
            if (message == null || message.length() == 0) {
                throw new IllegalArgumentException("Error message must not be null nor empty");
            }
            this.message = message;
            return this;
        }

        @Override
        public <V> V until(Function<? super WebDriver, V> isTrue) {
            if (message == null) {
                return super.until(isTrue);
            } else {
                try {
                    return super.until(isTrue);
                } catch (TimeoutException e) {
                    throw new TimeoutException(message, e);
                }
            }
        }
    }
}
