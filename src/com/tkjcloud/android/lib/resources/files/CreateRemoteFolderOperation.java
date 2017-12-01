/* tkjcloud Android Library is available under MIT license
 *   Copyright (C) 2015 tkjcloud Inc.
 *   
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:
 *   
 *   The above copyright notice and this permission notice shall be included in
 *   all copies or substantial portions of the Software.
 *   
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
 *   EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *   MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND 
 *   NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS 
 *   BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN 
 *   ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 *   CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *   THE SOFTWARE.
 *
 */

package com.tkjcloud.android.lib.resources.files;

import org.apache.jackrabbit.webdav.client.methods.MkColMethod;

import com.tkjcloud.android.lib.common.tkjcloudClient;
import com.tkjcloud.android.lib.common.network.WebdavUtils;
import com.tkjcloud.android.lib.common.operations.RemoteOperation;
import com.tkjcloud.android.lib.common.operations.RemoteOperationResult;
import com.tkjcloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.tkjcloud.android.lib.common.utils.Log_OC;
import com.tkjcloud.android.lib.resources.status.tkjcloudVersion;


/**
 * Remote operation performing the creation of a new folder in the tkjcloud server.
 *
 * @author David A. Velasco
 * @author masensio
 */
public class CreateRemoteFolderOperation extends RemoteOperation {

    private static final String TAG = CreateRemoteFolderOperation.class.getSimpleName();

    private static final int READ_TIMEOUT = 30000;
    private static final int CONNECTION_TIMEOUT = 5000;


    protected String mRemotePath;
    protected boolean mCreateFullPath;

    /**
     * Constructor
     *
     * @param remotePath     Full path to the new directory to create in the remote server.
     * @param createFullPath 'True' means that all the ancestor folders should be created
     *                       if don't exist yet.
     */
    public CreateRemoteFolderOperation(String remotePath, boolean createFullPath) {
        mRemotePath = remotePath;
        mCreateFullPath = createFullPath;
    }

    /**
     * Performs the operation
     *
     * @param client Client object to communicate with the remote tkjcloud server.
     */
    @Override
    protected RemoteOperationResult run(tkjcloudClient client) {
        RemoteOperationResult result = null;
        tkjcloudVersion version = client.gettkjcloudVersion();
        boolean versionWithForbiddenChars =
            (version != null && version.isVersionWithForbiddenCharacters());
        boolean noInvalidChars = FileUtils.isValidPath(mRemotePath, versionWithForbiddenChars);
        if (noInvalidChars) {
            result = createFolder(client);
            if (!result.isSuccess() && mCreateFullPath &&
                RemoteOperationResult.ResultCode.CONFLICT == result.getCode()) {
                result = createParentFolder(FileUtils.getParentPath(mRemotePath), client);
                if (result.isSuccess()) {
                    result = createFolder(client);    // second (and last) try
                }
            }

        } else {
            result = new RemoteOperationResult(ResultCode.INVALID_CHARACTER_IN_NAME);
        }

        return result;
    }


    private RemoteOperationResult createFolder(tkjcloudClient client) {
        RemoteOperationResult result = null;
        MkColMethod mkcol = null;
        try {
            mkcol = new MkColMethod(client.getWebdavUri() + WebdavUtils.encodePath(mRemotePath));
            client.executeMethod(mkcol, READ_TIMEOUT, CONNECTION_TIMEOUT);
            result = new RemoteOperationResult(mkcol.succeeded(), mkcol);
            Log_OC.d(TAG, "Create directory " + mRemotePath + ": " + result.getLogMessage());
            client.exhaustResponse(mkcol.getResponseBodyAsStream());

        } catch (Exception e) {
            result = new RemoteOperationResult(e);
            Log_OC.e(TAG, "Create directory " + mRemotePath + ": " + result.getLogMessage(), e);

        } finally {
            if (mkcol != null)
                mkcol.releaseConnection();
        }
        return result;
    }

    private RemoteOperationResult createParentFolder(String parentPath, tkjcloudClient client) {
        RemoteOperation operation = new CreateRemoteFolderOperation(parentPath,
            mCreateFullPath);
        return operation.execute(client);
    }


}
