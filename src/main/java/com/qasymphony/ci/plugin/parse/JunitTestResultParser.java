package com.qasymphony.ci.plugin.parse;

import com.qasymphony.ci.plugin.model.AutomationTestResult;
import com.qasymphony.ci.plugin.utils.LoggerUtils;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author trongle
 * @version 11/2/2015 9:13 AM trongle $
 * @since 1.0
 */
public class JunitTestResultParser {
  private static Logger LOG = Logger.getLogger(JunitTestResultParser.class.getName());

  private static final String JUNIT_PREFIX = "TEST-*";
  private static final String JUNIT_SUFIX = "/*.xml";

  /**
   * Parse junit result
   *
   * @param build
   * @param launcher
   * @param listener
   * @return
   * @throws Exception
   */
  public static List<AutomationTestResult> parse(AbstractBuild build, Launcher launcher, BuildListener listener)
    throws Exception {
    AbstractProject project = build.getProject();
    String basedDir = build.getWorkspace().toURI().getPath();
    LOG.info("Based dir is:" + basedDir);
    if (project.getClass().getName().toLowerCase().contains("maven")) {
      //if pom file is located at workspace, we do not scan
      FileSet fs = Util.createFileSet(new File(basedDir), MavenJunitParse.TEST_RESULT_LOCATIONS);
      DirectoryScanner ds = fs.getDirectoryScanner();
      if (ds.getIncludedFiles().length > 0)
        return new MavenJunitParse(build, launcher, listener).parse();
    }

    //we'll auto detect test result folder,when project is not maven style
    MavenJunitParse mavenJunitParse = new MavenJunitParse(build, launcher, listener);
    List<String> resultFolders = scanJunitTestResultFolder(basedDir);
    LOG.info("Scanning junit test result in dir:" + basedDir);
    LOG.info(String.format("Found: %s dirs, %s", resultFolders.size(), resultFolders));
    List<AutomationTestResult> result = new LinkedList<>();
    for (String res : resultFolders) {
      try {
        result.addAll(mavenJunitParse.parse(res + JUNIT_SUFIX));
      } catch (Exception e) {
        LoggerUtils.formatWarn(listener.getLogger(), "Try to scan test result in: %s, error: %s", res, e.getMessage());
      }
    }
    return result;
  }

  /**
   * Scan junit test result folder
   *
   * @param basedDir
   * @return
   */
  public static List<String> scanJunitTestResultFolder(String basedDir) {
    File currentBasedDir = new File(basedDir);
    FileSet fs = Util.createFileSet(new File(basedDir), JUNIT_PREFIX);
    DirectoryScanner ds = fs.getDirectoryScanner();
    List<String> resultFolders = new ArrayList<>();
    if (ds.getIncludedFiles().length > 0) {
      resultFolders.add("");
    } else {
      for (String notIncludedDirName : ds.getNotIncludedDirectories()) {
        if (!StringUtils.isEmpty(notIncludedDirName)) {
          File dirToScan = new File(currentBasedDir.getPath(), notIncludedDirName);
          FileSet subFileSet = Util.createFileSet(dirToScan, JUNIT_PREFIX);
          DirectoryScanner subDirScanner = subFileSet.getDirectoryScanner();
          if (subDirScanner.getIncludedFiles().length > 0) {
            resultFolders.add(notIncludedDirName);
          }
        }
      }
    }
    return resultFolders;
  }
}
