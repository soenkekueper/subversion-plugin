/*
 * The MIT License
 *
 * Copyright 2019 SET GmbH.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package jenkins.scm.impl.subversion;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNInfo;
import org.tmatesoft.svn.core.wc.SVNRevision;

import hudson.Extension;
import hudson.FilePath;
import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;

/**
 * Provides data from svn info.
 */
public final class SvninfoStep extends Step {

    private final String path;

    @DataBoundConstructor
    public SvninfoStep(final String path) {
        this.path = path;
    }

    public String getPath() {
        return this.path;
    }

    @Override
    public StepExecution start(final StepContext context) throws Exception {
        return new Execution(this.path, context);
    }

    @Extension
    public static final class DescriptorImpl extends StepDescriptor {

        @Override
        public String getFunctionName() {
            return "svninfo";
        }

        @Override
        public String getDisplayName() {
            return "Provides some data from svn info as a map.";
        }

        @Override
        public Set<Class<?>> getRequiredContext() {
            return Collections.<Class<?>>singleton(FilePath.class);
        }

    }

    static final class Execution extends SynchronousNonBlockingStepExecution<Map<String, String>> {

        private final String path;

        Execution(final String path, final StepContext context) {
            super(context);
            this.path = path;
        }

        @Override
        protected Map<String, String> run() throws Exception {
            final FilePath filepath = this.getContext().get(FilePath.class).child(this.path);
            return Collections.unmodifiableMap(filepath.act(new SvnInfoFileCallable()));
        }

        private static final long serialVersionUID = 1L;

    }

    private static final class SvnInfoFileCallable extends MasterToSlaveFileCallable<Map<String, String>> {

        private static final long serialVersionUID = 1456116737119973646L;

        @Override
        public Map<String, String> invoke(final File file, final VirtualChannel channel)
            throws IOException, InterruptedException {

            final SVNClientManager clientManager = SVNClientManager.newInstance();
            try {
                final SVNInfo info = clientManager.getWCClient().doInfo(file, SVNRevision.WORKING);
                final Map<String, String> result = new LinkedHashMap<String, String>();
                result.put("REVISION", Long.toString(info.getRevision().getNumber()));
                result.put("URL", info.getURL().toString());
                result.put("CHECKSUM", info.getChecksum());
                result.put("REPOSITORY_UUID", info.getRepositoryUUID());
                result.put("LAST_AUTHOR", info.getAuthor());
                result.put("LAST_CHANGE_REVISION", Long.toString(info.getCommittedRevision().getNumber()));
                return result;
            } catch (final SVNException e) {
                throw new IOException("failed to get svn infos for " + file, e);
            } finally {
                clientManager.dispose();
            }
        }
    }

}
