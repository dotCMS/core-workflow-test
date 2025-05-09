import { renderHook, act } from '@testing-library/react-hooks';

import { updateNavigation } from '@dotcms/client';
import { getUVEState, initUVE, createUVESubscription } from '@dotcms/uve';
import { DotCMSEditablePage, UVEEventType } from '@dotcms/uve/types';

import { useEditableDotCMSPage } from '../../hooks/useEditableDotCMSPage';

jest.mock('@dotcms/client', () => ({
    updateNavigation: jest.fn()
}));

jest.mock('@dotcms/uve', () => ({
    getUVEState: jest.fn(),
    initUVE: jest.fn(),
    createUVESubscription: jest.fn()
}));

describe('useEditableDotCMSPage', () => {
    const getUVEStateMock = getUVEState as jest.Mock;
    const initUVEMock = initUVE as jest.Mock;
    const createUVESubscriptionMock = createUVESubscription as jest.Mock;
    const updateNavigationMock = updateNavigation as jest.Mock;

    const mockUnsubscribe = jest.fn();
    const mockDestroyUVESubscriptions = jest.fn();

    // Use unknown as intermediate type to avoid type checking issues
    const mockEditablePage = {
        page: {
            pageURI: '/test-page',
            title: 'Test Page',
            metadata: {},
            template: 'test-template',
            modDate: '2023-01-01',
            cachettl: 0
        },
        content: {
            testContent: [{ title: 'Test Item' }]
        },
        graphql: {} // Required for DotCMSGraphQLPageResponse
    } as unknown as DotCMSEditablePage;

    beforeEach(() => {
        jest.clearAllMocks();
        initUVEMock.mockReturnValue({ destroyUVESubscriptions: mockDestroyUVESubscriptions });
        createUVESubscriptionMock.mockReturnValue({ unsubscribe: mockUnsubscribe });
    });

    test('should initialize with the provided editable page', () => {
        getUVEStateMock.mockReturnValue({ mode: 'EDIT' });

        const { result } = renderHook(() => useEditableDotCMSPage(mockEditablePage));

        expect(result.current).toEqual(mockEditablePage);
    });

    test('should initialize UVE and update navigation when UVE state exists', () => {
        getUVEStateMock.mockReturnValue({ mode: 'EDIT' });

        renderHook(() => useEditableDotCMSPage(mockEditablePage));

        expect(initUVEMock).toHaveBeenCalledWith(mockEditablePage);
        expect(updateNavigationMock).toHaveBeenCalledWith('/test-page');
    });

    test('should not initialize UVE when UVE state does not exist', () => {
        getUVEStateMock.mockReturnValue(undefined);

        renderHook(() => useEditableDotCMSPage(mockEditablePage));

        expect(initUVEMock).not.toHaveBeenCalled();
        expect(updateNavigationMock).not.toHaveBeenCalled();
    });

    test('should cleanup subscriptions on unmount', () => {
        getUVEStateMock.mockReturnValue({ mode: 'EDIT' });

        const { unmount } = renderHook(() => useEditableDotCMSPage(mockEditablePage));

        unmount();

        expect(mockDestroyUVESubscriptions).toHaveBeenCalled();
        expect(mockUnsubscribe).toHaveBeenCalled();
    });

    test('should update editable page when content changes are received', () => {
        getUVEStateMock.mockReturnValue({ mode: 'EDIT' });

        let contentChangesCallback: (payload: DotCMSEditablePage) => void;

        createUVESubscriptionMock.mockImplementation((eventType, callback) => {
            if (eventType === UVEEventType.CONTENT_CHANGES) {
                contentChangesCallback = callback;
            }

            return { unsubscribe: mockUnsubscribe };
        });

        const { result } = renderHook(() => useEditableDotCMSPage(mockEditablePage));

        const updatedPage = {
            page: {
                pageURI: '/test-page',
                title: 'Updated Title',
                metadata: {},
                template: 'test-template',
                modDate: '2023-01-01',
                cachettl: 0
            },
            content: {
                testContent: [{ title: 'Updated Item' }]
            },
            graphql: {} // Required for DotCMSGraphQLPageResponse
        } as unknown as DotCMSEditablePage;

        act(() => {
            contentChangesCallback(updatedPage);
        });

        expect(result.current).toEqual(updatedPage);
    });
});
