/*
 * Copyright 2016-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.facebook.buck.eden;

import com.facebook.buck.io.BuckPaths;
import com.facebook.buck.io.DefaultProjectFilesystemDelegate;
import com.facebook.buck.io.ProjectFilesystemDelegate;
import com.facebook.buck.util.sha1.Sha1HashCode;
import com.facebook.eden.EdenError;
import com.facebook.thrift.TException;

import java.io.IOException;
import java.nio.file.Path;

public final class EdenProjectFilesystemDelegate implements ProjectFilesystemDelegate {

  private final EdenMount mount;
  private final BuckPaths buckPaths;
  private final DefaultProjectFilesystemDelegate delegate;

  public EdenProjectFilesystemDelegate(EdenMount mount, BuckPaths buckPaths) {
    this.mount = mount;
    this.buckPaths = buckPaths;
    this.delegate = new DefaultProjectFilesystemDelegate(mount.getMountPoint());
  }

  @Override
  public Sha1HashCode computeSha1(Path pathRelativeToProjectRootOrJustAbsolute) throws IOException {
    Path fileToHash = getPathForRelativePath(pathRelativeToProjectRootOrJustAbsolute);

    if (fileToHash.startsWith(mount.getMountPoint())) {
      Path entry = mount.getMountPoint().relativize(fileToHash);
      // TODO(bolinfest): Generalize this to check if entry is under any of the Eden client's bind
      // mounts rather than hardcoding a test for buck-out/.
      if (!entry.startsWith(buckPaths.getBuckOut())) {
        try {
          return mount.getSha1(entry);
        } catch (TException | EdenError e) {
          throw new IOException(e);
        }
      }
    }

    return delegate.computeSha1(pathRelativeToProjectRootOrJustAbsolute);
  }

  @Override
  public Path getPathForRelativePath(Path pathRelativeToProjectRootOrJustAbsolute) {
    return delegate.getPathForRelativePath(pathRelativeToProjectRootOrJustAbsolute);
  }
}
