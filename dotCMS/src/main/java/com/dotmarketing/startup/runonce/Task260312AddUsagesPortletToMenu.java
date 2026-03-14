package com.dotmarketing.startup.runonce;

import com.dotcms.exception.ExceptionUtil;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.UtilMethods;

import java.util.List;
import java.util.Map;

import static com.dotmarketing.util.PortletID.SITES;
import static com.dotmarketing.util.PortletID.USAGE;

public class Task260312AddUsagesPortletToMenu implements StartupTask {

    @Override
    public boolean forceRun() {
       return true;
    }

    /**
     * Adds the custom {@code Usage} portlet to the appropriate Menu Group.
     *
     * @throws DotDataException An error occurred when adding the 'Usage' portlet.
     */
    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        try {
            DbConnectionFactory.getConnection().setAutoCommit(true);
            final DotConnect dotConnect = new DotConnect();
            dotConnect.executeStatement("ALTER TABLE identifier DROP COLUMN asset_subtype");
        } catch (SQLException exception) {
            throw new DotDataException(exception.getMessage(),exception);
        }
    }
}
}
