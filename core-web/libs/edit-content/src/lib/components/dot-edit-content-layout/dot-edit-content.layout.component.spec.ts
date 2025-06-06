import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator,
    SpyObject
} from '@ngneat/spectator/jest';
import { MockComponent } from 'ng-mocks';
import { of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { fakeAsync, tick } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';

import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialog } from 'primeng/confirmdialog';
import { DialogService } from 'primeng/dynamicdialog';
import { MessagesModule } from 'primeng/messages';

import {
    DotContentletService,
    DotContentTypeService,
    DotCurrentUserService,
    DotHttpErrorManagerService,
    DotLanguagesService,
    DotMessageService,
    DotWorkflowActionsFireService,
    DotWorkflowsActionsService,
    DotWorkflowService
} from '@dotcms/data-access';
import { DotLanguage } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';
import {
    MOCK_MULTIPLE_WORKFLOW_ACTIONS,
    MOCK_SINGLE_WORKFLOW_ACTIONS
} from '@dotcms/utils-testing';

import { DotEditContentLayoutComponent } from './dot-edit-content.layout.component';

import { DotEditContentService } from '../../services/dot-edit-content.service';
import { DotEditContentStore } from '../../store/edit-content.store';
import { MOCK_CONTENTLET_1_TAB } from '../../utils/edit-content.mock';
import * as utils from '../../utils/functions.util';
import { CONTENT_TYPE_MOCK } from '../../utils/mocks';
import { DotEditContentFormComponent } from '../dot-edit-content-form/dot-edit-content-form.component';
import { DotEditContentSidebarComponent } from '../dot-edit-content-sidebar/dot-edit-content-sidebar.component';

const MOCK_LANGUAGES = [{ id: 1, isoCode: 'en-us', defaultLanguage: false }] as DotLanguage[];

describe('EditContentLayoutComponent', () => {
    let spectator: Spectator<DotEditContentLayoutComponent>;

    let store: SpyObject<InstanceType<typeof DotEditContentStore>>;
    let dotContentTypeService: SpyObject<DotContentTypeService>;
    let dotEditContentService: SpyObject<DotEditContentService>;
    let workflowActionsService: SpyObject<DotWorkflowsActionsService>;
    let dotLanguagesService: SpyObject<DotLanguagesService>;

    const createComponent = createComponentFactory({
        component: DotEditContentLayoutComponent,
        imports: [
            MessagesModule,
            ButtonModule,
            MockComponent(DotEditContentFormComponent),
            MockComponent(DotEditContentSidebarComponent),
            DotMessagePipe
        ],
        componentProviders: [
            DotEditContentStore,
            mockProvider(DotWorkflowsActionsService),
            mockProvider(DotWorkflowActionsFireService),
            mockProvider(DotEditContentService),
            mockProvider(DotContentTypeService),
            mockProvider(DotWorkflowService),
            mockProvider(DotContentletService)
        ],
        providers: [
            mockProvider(DotHttpErrorManagerService),
            mockProvider(MessageService),
            mockProvider(DialogService),
            mockProvider(DotLanguagesService),
            {
                provide: DotCurrentUserService,
                useValue: {
                    getCurrentUser: () =>
                        of({
                            userId: '123',
                            userName: 'John Doe'
                        })
                }
            },
            {
                provide: ActivatedRoute,
                useValue: {
                    get snapshot() {
                        return { params: { id: undefined, contentType: undefined } };
                    }
                }
            },
            mockProvider(Router, {
                navigate: jest.fn().mockReturnValue(Promise.resolve(true)),
                url: '/test-url',
                events: of()
            }),
            provideHttpClient(),
            provideHttpClientTesting(),
            mockProvider(DotMessageService)
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            detectChanges: false
        });

        spectator.detectChanges();

        store = spectator.inject(DotEditContentStore, true);
        dotContentTypeService = spectator.inject(DotContentTypeService, true);
        workflowActionsService = spectator.inject(DotWorkflowsActionsService, true);
        dotEditContentService = spectator.inject(DotEditContentService, true);
        dotLanguagesService = spectator.inject(DotLanguagesService, true);

        jest.spyOn(dotLanguagesService, 'get').mockReturnValue(of(MOCK_LANGUAGES));

        // Mock the initial UI state
        jest.spyOn(utils, 'getStoredUIState').mockReturnValue({
            activeTab: 0,
            isSidebarOpen: true,
            activeSidebarTab: 0,
            isBetaMessageVisible: true
        });

        dotContentTypeService.updateContentType.mockReturnValue(of(CONTENT_TYPE_MOCK));
    });

    it('should have p-confirmDialog component', () => {
        expect(spectator.query(ConfirmDialog)).toBeTruthy();
    });

    describe('New Content Editor', () => {
        it('should initialize new content, show layout components and dialogs when new content editor is enabled', fakeAsync(() => {
            dotContentTypeService.getContentType.mockReturnValue(of(CONTENT_TYPE_MOCK));
            workflowActionsService.getDefaultActions.mockReturnValue(
                of(MOCK_SINGLE_WORKFLOW_ACTIONS)
            );
            workflowActionsService.getWorkFlowActions.mockReturnValue(
                of(MOCK_SINGLE_WORKFLOW_ACTIONS)
            );

            store.initializeNewContent('contentTypeName');

            spectator.detectChanges();

            tick(); // Wait for the defer to load

            expect(store.isEnabledNewContentEditor()).toBe(true);
            expect(store.isSidebarOpen()).toBe(true);

            expect(spectator.query(byTestId('edit-content-layout__topBar'))).toBeTruthy();
            expect(spectator.query(byTestId('edit-content-layout__body'))).toBeTruthy();
            expect(spectator.query(byTestId('edit-content-layout__sidebar'))).toBeTruthy();

            expect(spectator.query(ConfirmDialog)).toBeTruthy();
        }));

        describe('Beta Message', () => {
            beforeEach(fakeAsync(() => {
                dotContentTypeService.getContentType.mockReturnValue(of(CONTENT_TYPE_MOCK));
                workflowActionsService.getDefaultActions.mockReturnValue(
                    of(MOCK_SINGLE_WORKFLOW_ACTIONS)
                );
                store.initializeNewContent('contentTypeName');
                spectator.detectChanges();
                tick();
            }));

            it('should show beta message by default', fakeAsync(() => {
                spectator.detectChanges();
                tick();
                expect(spectator.query(byTestId('edit-content-layout__beta-message'))).toBeTruthy();
                expect(
                    spectator.query(byTestId('edit-content-layout__beta-message-content'))
                ).toBeTruthy();
                expect(
                    spectator.query(byTestId('edit-content-layout__beta-message-link'))
                ).toBeTruthy();
                expect(
                    spectator.query(byTestId('edit-content-layout__beta-message-close-button'))
                ).toBeTruthy();
            }));

            it('should hide beta message when close button is clicked', fakeAsync(() => {
                spectator.detectChanges();
                tick();

                const closeButton = spectator.query(
                    byTestId('edit-content-layout__beta-message-close-button')
                ) as HTMLButtonElement;
                expect(closeButton).toBeTruthy();

                spectator.click(closeButton);
                spectator.detectChanges();
                tick();

                expect(spectator.query(byTestId('edit-content-layout__beta-message'))).toBeFalsy();
            }));

            it('should have correct link to old editor', async () => {
                // Initialize the content type
                dotContentTypeService.getContentType.mockReturnValue(of(CONTENT_TYPE_MOCK));
                workflowActionsService.getDefaultActions.mockReturnValue(
                    of(MOCK_SINGLE_WORKFLOW_ACTIONS)
                );
                store.initializeNewContent('contentTypeName');

                // Wait for initialization
                await spectator.fixture.whenStable();
                spectator.detectChanges();

                // Create a fake event with preventDefault
                const event = new MouseEvent('click');
                Object.defineProperty(event, 'preventDefault', { value: jest.fn() });

                // Spy on the store method
                const disableNewContentEditorSpy = jest.spyOn(store, 'disableNewContentEditor');

                const link = spectator.query(
                    byTestId('edit-content-layout__beta-message-link')
                ) as HTMLAnchorElement;

                // Dispatch the event
                link.dispatchEvent(event);

                expect(event.preventDefault).toHaveBeenCalled();
                expect(disableNewContentEditorSpy).toHaveBeenCalled();
            });
        });

        it('should not show top bar message when new content editor is disabled', () => {
            const CONTENT_TYPE_MOCK_NO_METADATA = {
                ...CONTENT_TYPE_MOCK,
                metadata: undefined
            };

            dotContentTypeService.getContentType.mockReturnValue(of(CONTENT_TYPE_MOCK_NO_METADATA));
            workflowActionsService.getDefaultActions.mockReturnValue(
                of(MOCK_SINGLE_WORKFLOW_ACTIONS)
            );
            workflowActionsService.getWorkFlowActions.mockReturnValue(
                of(MOCK_SINGLE_WORKFLOW_ACTIONS)
            );
            store.initializeNewContent('contentTypeName');

            spectator.detectChanges();
            expect(store.isEnabledNewContentEditor()).toBe(false);
            expect(spectator.query(byTestId('edit-content-layout__beta-message'))).toBeFalsy();
        });
    });

    describe('Warning Messages', () => {
        beforeEach(() => {
            dotContentTypeService.getContentType.mockReturnValue(of(CONTENT_TYPE_MOCK));
            workflowActionsService.getDefaultActions.mockReturnValue(
                of(MOCK_SINGLE_WORKFLOW_ACTIONS)
            );
            dotEditContentService.getContentById.mockReturnValue(of(MOCK_CONTENTLET_1_TAB));
        });

        it('should show workflow warning message when multiple schemes are available for new content', fakeAsync(() => {
            // Multiple schemes trigger the warning message
            workflowActionsService.getDefaultActions.mockReturnValue(
                of(MOCK_MULTIPLE_WORKFLOW_ACTIONS)
            );

            store.initializeNewContent('contentTypeName');
            spectator.detectChanges();
            tick();

            const warningMessage = spectator.query(
                byTestId('edit-content-layout__select-workflow-warning')
            );
            expect(store.showSelectWorkflowWarning()).toBe(true);
            expect(warningMessage).toBeTruthy();
        }));

        it('should not show workflow warning message when only one scheme is available', fakeAsync(() => {
            // Set up single workflow scheme
            workflowActionsService.getWorkFlowActions.mockReturnValue(
                of(MOCK_SINGLE_WORKFLOW_ACTIONS)
            );

            store.initializeNewContent('contentTypeName');
            spectator.detectChanges();
            tick();

            const warningMessage = spectator.query(
                byTestId('edit-content-layout__select-workflow-warning')
            );
            expect(warningMessage).toBeFalsy();
        }));

        it('should not show workflow warning message for existing content', fakeAsync(() => {
            // Even with multiple schemes, existing content shouldn't show warning
            workflowActionsService.getWorkFlowActions.mockReturnValue(
                of(MOCK_MULTIPLE_WORKFLOW_ACTIONS)
            );

            store.initializeExistingContent('123');
            spectator.detectChanges();
            tick();

            const warningMessage = spectator.query(
                byTestId('edit-content-layout__select-workflow-warning')
            );
            expect(warningMessage).toBeFalsy();
        }));

        describe('Warning Messages', () => {
            beforeEach(fakeAsync(() => {
                dotContentTypeService.getContentType.mockReturnValue(of(CONTENT_TYPE_MOCK));
                workflowActionsService.getDefaultActions.mockReturnValue(
                    of(MOCK_SINGLE_WORKFLOW_ACTIONS)
                );
                store.initializeNewContent('contentTypeName');
                spectator.detectChanges();
                tick();
            }));

            it('should show lock warning message when lockWarningMessage signal returns a message', fakeAsync(() => {
                const mockMessage = 'Lock warning message';
                jest.spyOn(store, 'lockWarningMessage').mockReturnValue(mockMessage);
                spectator.detectChanges();
                tick();

                const warningElement = spectator.query(
                    byTestId('edit-content-layout__lock-warning')
                );
                const warningContent = spectator.query(
                    byTestId('edit-content-layout__lock-warning-content')
                );

                expect(warningElement).toBeTruthy();
                expect(warningContent).toBeTruthy();
                expect(warningContent.innerHTML).toContain(mockMessage);
            }));

            it('should show select workflow warning when showSelectWorkflowWarning signal returns true', fakeAsync(() => {
                jest.spyOn(store, 'showSelectWorkflowWarning').mockReturnValue(true);
                spectator.detectChanges();
                tick();

                const warningElement = spectator.query(
                    byTestId('edit-content-layout__select-workflow-warning')
                );
                const selectWorkflowLink = spectator.query(byTestId('select-workflow-link'));

                expect(warningElement).toBeTruthy();
                expect(selectWorkflowLink).toBeTruthy();
            }));

            it('should trigger selectWorkflow when clicking on workflow warning link', fakeAsync(() => {
                jest.spyOn(store, 'showSelectWorkflowWarning').mockReturnValue(true);
                spectator.detectChanges();
                tick();

                const selectWorkflowLink = spectator.query(byTestId('select-workflow-link'));
                expect(selectWorkflowLink).toBeTruthy();

                const event = new MouseEvent('click');
                Object.defineProperty(event, 'preventDefault', { value: jest.fn() });
                selectWorkflowLink.dispatchEvent(event);

                expect(event.preventDefault).toHaveBeenCalled();

                // Verify that the showDialog signal was set to true
                expect(spectator.component.$showDialog()).toBe(true);
            }));
        });
    });
});
