import '@testing-library/jest-dom';

import { render, screen } from '@testing-library/react';

import { Container } from './Container';

import { MockContextRender, mockPageContext } from '../../mocks/mockPageContext';

describe('Container', () => {
    // Mock data for your context and container

    describe('with contentlets', () => {
        const mockContainerRef = {
            identifier: 'container-1',
            uuid: '1',
            containers: []
        };

        it('renders NoContent component for unsupported content types', () => {
            const updatedContext = {
                ...mockPageContext,
                components: {},
                isInsideEditor: true
            };

            render(
                <MockContextRender mockContext={updatedContext}>
                    <Container containerRef={mockContainerRef} />
                </MockContextRender>
            );

            expect(screen.getByTestId('no-component')).toHaveTextContent(
                'No Component for content-type-1'
            );
        });

        describe('without contentlets', () => {
            const mockContainerRef = {
                identifier: 'container-2',
                uuid: '2',
                containers: []
            };
            it('renders EmptyContainer component in editor mode', () => {
                const updatedContext = {
                    ...mockPageContext,
                    components: {},
                    isInsideEditor: true
                };
                render(
                    <MockContextRender mockContext={updatedContext}>
                        <Container containerRef={mockContainerRef} />
                    </MockContextRender>
                );

                expect(screen.getByTestId('empty-container')).toHaveTextContent(
                    'This container is empty.'
                );
            });

            it('dont render EmptyContainer component outside editor mode', () => {
                const updatedContext = {
                    ...mockPageContext,
                    components: {},
                    isInsideEditor: false
                };
                render(
                    <MockContextRender mockContext={updatedContext}>
                        <Container containerRef={mockContainerRef} />
                    </MockContextRender>
                );

                expect(screen.queryByTestId('empty-container')).toBeNull();
            });
        });
    });

    // Add tests for pointer events, dynamic component rendering, and other scenarios...
});