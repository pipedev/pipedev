package ca.on.oicr.pde.testing.decider;

import ca.on.oicr.pde.dao.SeqwareService;
import ca.on.oicr.pde.model.Accessionable;
import ca.on.oicr.pde.model.ReducedFileProvenanceReportRecord;
import ca.on.oicr.pde.model.SeqwareAccession;
import ca.on.oicr.pde.model.Workflow;
import ca.on.oicr.pde.model.WorkflowRun;
import ca.on.oicr.pde.model.WorkflowRunReportRecord;
import ca.on.oicr.pde.testing.common.RunTestBase;
import ca.on.oicr.pde.utilities.Timer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Joiner;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.testng.Assert;
import org.testng.annotations.*;
import org.testng.annotations.Test;

@Listeners({ca.on.oicr.pde.testing.testng.TestCaseReporter.class})
public class DeciderRunTest extends RunTestBase {

    private final Logger log = LogManager.getLogger(DeciderRunTest.class);
    private final static List<File> reports = Collections.synchronizedList(new ArrayList<File>());

    private final File deciderJar;
    private final String deciderClass;
    private final File bundledWorkflow;

    SeqwareService seqwareService;
    SeqwareAccession workflowSwid;

    private final List<String> studies = new ArrayList<String>();
    private final List<String> sequencerRuns = new ArrayList<String>();
    private final List<String> samples = new ArrayList<String>();

    File actualReportFile;
    File expectedReportFile;

    DeciderRunTestReport actual;
    DeciderRunTestReport expected;

    DeciderRunTestDefinition.Test testDefinition;

    private Timer executionTimer;

    public DeciderRunTest(SeqwareService seqwareService, File seqwareDistribution, File seqwareSettings, File workingDirectory, String testName,
            File deciderJar, File bundledWorkflow, String deciderClass, DeciderRunTestDefinition.Test definition) throws IOException {

        super(seqwareDistribution, seqwareSettings, workingDirectory, testName);

        this.seqwareService = seqwareService;
        this.deciderJar = deciderJar;
        this.bundledWorkflow = bundledWorkflow;
        this.deciderClass = deciderClass;
        this.testDefinition = definition;

        studies.addAll(testDefinition.getStudies());
        samples.addAll(testDefinition.getSamples());
        sequencerRuns.addAll(testDefinition.getSequencerRuns());

        expectedReportFile = testDefinition.metrics();

        if (expectedReportFile != null) {
            try {
                expected = DeciderRunTestReport.buildFromJson(expectedReportFile);
            } catch (IOException ioe) {
                log.printf(Level.WARN, "[%s] There was a problem loading the metrics file: [%s].\n"
                        + "The exception output:\n%s\nContinuing with test but comparision step will fail.",
                        testName, expectedReportFile.getAbsolutePath(), ioe.toString());
                expected = null;
            }
        } else {
            log.printf(Level.WARN, "[%s] Missing an expected output metrics file. Skipping comparison step", testName);
        }

    }

    @BeforeSuite
    public void beforeAllRunTests() {
        //
    }

    @BeforeClass
    public void beforeEachRunTest() throws IOException {

        log.printf(Level.INFO, "[%s] Starting run test", testName);
        executionTimer = Timer.start();
        Assert.assertNotNull(seqwareDistribution,
                "Seqware distribution path is not set - set seqwareDistribution in pom.xml.");
        Assert.assertNotNull(workingDirectory,
                "Working directory path is not set - set workingDirectory in pom.xml.");
        Assert.assertTrue(FileUtils.getFile(seqwareDistribution).exists(),
                "Seqware distribution does not exist - verify seqwareDistribution is correct in pom.xml.");
        Assert.assertTrue(seqwareSettings.exists(),
                "Generate seqware settings failed - please verify seqwareDirectory is accessible");

    }

    @BeforeMethod
    public void beforeEachTestMethod() throws IOException {
        //
    }

    @AfterMethod
    public void afterEachTestMethod() throws IOException {
        //
    }

    @AfterClass
    public void afterEachRunTest() throws IOException {

        Workflow.Builder workflowBuilder = new Workflow.Builder();
        workflowBuilder.setSwid(workflowSwid.toString());
        Workflow workflow = workflowBuilder.build();

        /* Cancel all submitted workflow runs
         * Each decider test run installs a separate instance of its associated
         * workflow bundle. So each decider run test has a unique workflow swid.
         */
        Timer timer = Timer.start();
        seqwareExecutor.cancelWorkflowRuns(workflow);
        log.printf(Level.INFO, "[%s] Completed clean up in %s", testName, timer.stop());

        log.printf(Level.INFO, "[%s] Test summary:\nRun time: %s\nWorking directory: %s",
                testName, executionTimer.stop(), workingDirectory);

    }

    @AfterSuite
    public void afterAllRunTests() {

        log.warn("Report file paths: " + reports.toString());
        log.warn("cp " + Joiner.on(" ").join(reports) + " " + "/tmp");

    }

    @Test(groups = "preExecution")
    public void initializeEnvironment() throws IOException {

    }

    @Test(groups = "preExecution")
    public void installWorkflow() throws IOException {
        Timer timer = Timer.start();
        workflowSwid = seqwareExecutor.installWorkflow(bundledWorkflow);
        Assert.assertNotNull(workflowSwid, "Installation of the workflow bundle failed");
        log.printf(Level.INFO, "[%s] Completed installing workflow bundle in %s", testName, timer.stop());
    }

//    @Test(groups = "preExecution", expectedExceptions = Exception.class)
//    public void getDeciderObject() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
//        //get decider object + all parameters
//        System.out.println(deciderClass);
//        Class c = Class.forName(deciderClass);
//        BasicDecider b = (BasicDecider) c.newInstance();
//        System.out.println("Metatype: " + b.getMetaType());
//        System.out.println("Syntax" + b.get_syntax());
//
//        b.setParams(Arrays.asList("--study", "AshPC"));
//        b.parse_parameters();
//        ReturnValue rv = b.init();
//
//        System.out.println("rv: " + rv.getParameters());
//
//        System.out.println("Metatype: " + b.getMetaType());
//        System.out.println("Syntax" + b.get_syntax());
//
//    }
    @Test(dependsOnGroups = "preExecution", groups = "execution")
    public void executeDecider() throws IOException, InstantiationException, ClassNotFoundException, IllegalAccessException {
        Timer timer = Timer.start();
        StringBuilder extraArgs = new StringBuilder();
        for (Entry<String, String> e : testDefinition.getParameters().entrySet()) {
            extraArgs.append(" ").append(e.getKey()).append(" ").append(e.getValue());
        }
        seqwareExecutor.deciderRunSchedule(deciderJar, workflowSwid, studies, sequencerRuns, samples, extraArgs.toString());
        log.printf(Level.INFO, "[%s] Completed workflow run scheduling in %s", testName, timer.stop());
    }

    @Test(dependsOnGroups = "execution", groups = "postExecution")
    public void calculateWorkflowRunReport() throws JsonProcessingException, IOException {
        Timer timer = Timer.start();
        Workflow.Builder workflowBuilder = new Workflow.Builder();
        workflowBuilder.setSwid(workflowSwid.getSwid());
        actual = generateReport(workflowBuilder.build());

        actualReportFile = new File(workingDirectory.getAbsolutePath() + "/" + testDefinition.outputName());
        Assert.assertFalse(actualReportFile.exists());

        FileUtils.write(actualReportFile, actual.toJson());

        reports.add(actualReportFile);

        log.printf(Level.INFO, "[%s] Completed generating workflow run report in %s", testName, timer.stop());
    }

    @Test(dependsOnGroups = "execution", dependsOnMethods = "calculateWorkflowRunReport", groups = "postExecution")
    public void compareWorkflowRunReport() throws JsonProcessingException, IOException {
        Timer timer = Timer.start();

        Assert.assertNotNull(expected, "There is no expected output to compare to");

        List<String> problems = validateReport(actual);
        Assert.assertTrue(problems.isEmpty(), problems.toString());

        if (!actual.equals(expected)) {
            Assert.fail(String.format("There are differences between decider runs:%nExpected run report: %s%nActual run report: %s%n%s",
                    expectedReportFile, actualReportFile, DeciderRunTestReport.diffReportSummary(actual, expected, 3)));
        }

        log.printf(Level.INFO, "[%s] Completed comparing workflow run reports in %s", testName, timer.stop());
    }

    private List<String> validateReport(DeciderRunTestReport t) {
        List<String> problems = new ArrayList<String>();

        if (t.getWorkflowRunCount().equals(Integer.valueOf("0"))) {
            problems.add("No workflow run were scheduled.");
        }

        return problems;
    }

    //TODO: move this to a separate implementation class of "Decider Report"
    private DeciderRunTestReport generateReport(Workflow w) {
        List<WorkflowRunReportRecord> wrrs = seqwareService.getWorkflowRunRecords(w);

        DeciderRunTestReport t = new DeciderRunTestReport();
        t.setWorkflowRunCount(wrrs.size());

        for (WorkflowRunReportRecord wrr : wrrs) {

            //TODO: get workflow run object from workflow run report record
            WorkflowRun.Builder workflowRunBuilder = new WorkflowRun.Builder();
            workflowRunBuilder.setSwid(wrr.getWorkflowRunSwid());
            WorkflowRun wr = workflowRunBuilder.build();

            //Get the workflow run's parent accession(s) (processing accession(s))
            List<Accessionable> parentAccessions = seqwareService.getParentAccessions(wr);

            //Get the workflow run's input file(s) (file accession(s))
            List<Accessionable> inputFileAccessions = seqwareService.getInputFileAccessions(wr);

            //TODO: 0.13.x series deciders do not use input_files, generalize parent and input file accessions
            if (inputFileAccessions.isEmpty()) {
                log.printf(Level.WARN, "[%s] Overriding input file accessions with parent accessions, workflow run swid=[%s]",
                        testName, wr.getSwid());
                inputFileAccessions = parentAccessions;
            }

            t.addStudies(seqwareService.getStudy(parentAccessions));
            t.addSamples(seqwareService.getSamples(parentAccessions));
            t.addSequencerRuns(seqwareService.getSequencerRuns(parentAccessions));
            t.addLanes(seqwareService.getLanes(parentAccessions));

            t.addWorkflows(seqwareService.getWorkflows(inputFileAccessions));
            t.addProcessingAlgorithms(seqwareService.getProcessingAlgorithms(inputFileAccessions));
            t.addFileMetaTypes(seqwareService.getFileMetaTypes(inputFileAccessions));

            List<ReducedFileProvenanceReportRecord> files = seqwareService.getFiles(inputFileAccessions);
            if (files.size() > t.getMaxInputFiles()) {
                t.setMaxInputFiles(files.size());
            }

            if (files.size() < t.getMinInputFiles()) {
                t.setMinInputFiles(files.size());
            }

            Map ini = seqwareService.getWorkflowRunIni(wr);
            for (String s : testDefinition.getIniExclusions()) {
                ini.remove(s);
            }

            WorkflowRunReport x = new WorkflowRunReport();
            x.setWorkflowIni(ini);
            x.setFiles(files);

            t.addWorkflowRun(x);

        }

        return t;

    }

}
