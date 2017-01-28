/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.debugger.stepping;

import com.intellij.debugger.engine.DebugProcessImpl;
import com.intellij.debugger.engine.MethodFilter;
import com.intellij.debugger.engine.RequestHint;
import com.intellij.debugger.engine.SuspendContextImpl;
import com.intellij.debugger.jdi.ThreadReferenceProxyImpl;
import com.sun.jdi.request.StepRequest;
import org.jetbrains.annotations.NotNull;

public class DebugProcessImplHelper {
    public static DebugProcessImpl.StepOverCommand createStepOverCommandWithCustomFilter(
            SuspendContextImpl suspendContext,
            final boolean ignoreBreakpoints,
            final MethodFilter methodFilter) {
        final DebugProcessImpl debugProcess = suspendContext.getDebugProcess();
        return debugProcess.new StepOverCommand(suspendContext, ignoreBreakpoints, StepRequest.STEP_LINE) {
            @NotNull
            @Override
            protected RequestHint getHint(SuspendContextImpl suspendContext, ThreadReferenceProxyImpl stepThread) {
                RequestHint hint = new MyRequestHint(stepThread, suspendContext, methodFilter); // TODO: Ask for constructor with step size
                hint.setRestoreBreakpoints(ignoreBreakpoints);
                hint.setIgnoreFilters(ignoreBreakpoints || debugProcess.getSession().shouldIgnoreSteppingFilters());

                return hint;
            }
        };
    }


    private static class MyRequestHint extends RequestHint {
        public MyRequestHint(ThreadReferenceProxyImpl stepThread, SuspendContextImpl suspendContext, MethodFilter methodFilter) {
            super(stepThread, suspendContext, methodFilter);
        }

        @Override
        public boolean isTheSameFrame(SuspendContextImpl context) {
            return super.isTheSameFrame(context);
        }
    }
}
