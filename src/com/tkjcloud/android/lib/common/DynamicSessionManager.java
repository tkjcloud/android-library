package com.tkjcloud.android.lib.common;

import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;

import com.tkjcloud.android.lib.common.accounts.AccountUtils;
import com.tkjcloud.android.lib.resources.status.tkjcloudVersion;

import java.io.IOException;

/**
 * Dynamic implementation of {@link tkjcloudClientManager}.
 * <p>
 * Wraps instances of {@link SingleSessionManager} and {@link SimpleFactoryManager} and delegates on one
 * or the other depending on the known version of the server corresponding to the {@link tkjcloudAccount}
 *
 * @author David A. Velasco
 */

public class DynamicSessionManager implements tkjcloudClientManager {

    private SimpleFactoryManager mSimpleFactoryManager = new SimpleFactoryManager();

    private SingleSessionManager mSingleSessionManager = new SingleSessionManager();

    @Override
    public tkjcloudClient getClientFor(tkjcloudAccount account, Context context)
            throws AccountUtils.AccountNotFoundException,
            OperationCanceledException, AuthenticatorException, IOException {

        tkjcloudVersion tkjcloudVersion = null;
        if (account.getSavedAccount() != null) {
            tkjcloudVersion = AccountUtils.getServerVersionForAccount(
                    account.getSavedAccount(), context
            );
        }

        if (tkjcloudVersion != null && tkjcloudVersion.isPreemptiveAuthenticationPreferred()) {
            return mSingleSessionManager.getClientFor(account, context);
        } else {
            return mSimpleFactoryManager.getClientFor(account, context);
        }
    }

    @Override
    public tkjcloudClient removeClientFor(tkjcloudAccount account) {
        tkjcloudClient clientRemovedFromFactoryManager = mSimpleFactoryManager.removeClientFor(account);
        tkjcloudClient clientRemovedFromSingleSessionManager = mSingleSessionManager.removeClientFor(account);
        if (clientRemovedFromSingleSessionManager != null) {
            return clientRemovedFromSingleSessionManager;
        } else {
            return clientRemovedFromFactoryManager;
        }
        // clientRemoved and clientRemoved2 should not be != null at the same time
    }

    @Override
    public void saveAllClients(Context context, String accountType)
            throws AccountUtils.AccountNotFoundException,
            AuthenticatorException, IOException, OperationCanceledException {
        mSimpleFactoryManager.saveAllClients(context, accountType);
        mSingleSessionManager.saveAllClients(context, accountType);
    }

}