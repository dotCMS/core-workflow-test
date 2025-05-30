import { ApplicationConfig, InjectionToken } from '@angular/core';
import { provideRouter } from '@angular/router';
import { createDotCMSClient } from '@dotcms/client/next';

import { provideDotCMSImageLoader } from '@dotcms/angular';

import { routes } from './app.routes';
import { environment } from '../environments/environment';
import { DotCMSClientConfig } from '@dotcms/types';
import { DotCMSEditablePageService } from '@dotcms/angular/next';

export const DOTCMS_CLIENT_TOKEN = new InjectionToken<ReturnType<typeof createDotCMSClient>>(
    'DOTCMS_CLIENT'
);

const DOTCMS_CLIENT_CONFIG: DotCMSClientConfig = {
    dotcmsUrl: environment.dotcmsUrl,
    authToken: environment.authToken,
    siteId: environment.siteId
};

const client = createDotCMSClient(DOTCMS_CLIENT_CONFIG);

export const appConfig: ApplicationConfig = {
    providers: [
        provideRouter(routes),
        /**
         * We provide the ⁠DOTCMS_CLIENT_TOKEN with the initialized ⁠DotCmsClient instance, enabling
         * its injection throughout the application. This approach ensures a single ⁠DotCmsClient
         * instance is used, promoting consistency and centralized management of client configuration.
         */
        {
            provide: DOTCMS_CLIENT_TOKEN,
            useValue: client
        },
        /**
         * This custom image loader, designed for the NgOptimizedImage component, appends the dotCMS URL
         * to the image source if it’s not an external URL.
         *
         * Additionally, it appends the ⁠language_id query parameter if the ⁠loaderParams object contains
         * a ⁠languageId key. To use an image from an external URL, set the ⁠isOutsideSRC key to ⁠true in
         * the ⁠loaderParams object.
         * <img [ngSrc]="https://my-url.com/some.jpg" [loaderParams]="{isOutsideSRC: true}" />
         * For further customization, you can provide your own image loader implementation.
         */
        provideDotCMSImageLoader(environment.dotcmsUrl),
        DotCMSEditablePageService
    ]
};
