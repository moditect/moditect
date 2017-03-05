/***
 * ASM: a very small and fast Java bytecode manipulation framework
 * Copyright (c) 2000-2011 INRIA, France Telecom
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holders nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.objectweb.asm.optimizer;

import org.objectweb.asm.ModuleVisitor;
import org.objectweb.asm.Opcodes;

/**
 * A {@link ModuleVisitor} that collects the {@link Constant}s of the
 * module declaration it visits.
 * 
 * @author Remi Forax
 */
public class ModuleConstantsCollector extends ModuleVisitor {

    private final ConstantPool cp;

    public ModuleConstantsCollector(final ModuleVisitor mv, final ConstantPool cp) {
        super(Opcodes.ASM6, mv);
        this.cp = cp;
    }

    @Override
    public void visitMainClass(String mainClass) {
        cp.newUTF8("ModuleMainClass");
        cp.newUTF8(mainClass);
        mv.visitMainClass(mainClass);
    }
    @Override
    public void visitTarget(String osName, String osArch,
            String osVersion) {
        cp.newUTF8("ModuleTarget");
        cp.newUTF8(osName);
        cp.newUTF8(osArch);
        cp.newUTF8(osVersion);
        super.visitTarget(osName, osArch, osVersion);
    }
    @Override
    public void visitPackage(String packaze) {
        cp.newUTF8("ModulePackages");
        cp.newUTF8(packaze);
        super.visitPackage(packaze);
    }
    
    @Override
    public void visitRequire(String module, int access, String version) {
        cp.newModule(module);
        cp.newUTF8(version);
        mv.visitRequire(module, access, version);
    }
    
    @Override
    public void visitExport(String packaze, int access, String... modules) {
        cp.newPackage(packaze);
        if (modules != null && modules.length > 0) {
            for(String module: modules) {
                cp.newModule(module);
            }
        }
        mv.visitExport(packaze, access, modules);
    }
    
    @Override
    public void visitOpen(String packaze, int access, String... modules) {
        cp.newPackage(packaze);
        if (modules != null && modules.length > 0) {
            for(String module: modules) {
                cp.newModule(module);
            }
        }
        mv.visitOpen(packaze, access, modules);
    }

    @Override
    public void visitUse(String service) {
        cp.newClass(service);
        mv.visitUse(service);
    }
    
    @Override
    public void visitProvide(String service, String... providers) {
        cp.newClass(service);
        for(String provider: providers) {
            cp.newClass(provider);
        }
        mv.visitProvide(service, providers);
    }
    
    @Override
    public void visitEnd() {
        mv.visitEnd();
    }
}
