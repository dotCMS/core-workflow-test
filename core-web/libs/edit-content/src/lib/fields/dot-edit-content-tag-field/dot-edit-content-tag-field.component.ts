import { BehaviorSubject, EMPTY } from 'rxjs';

import { CommonModule } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    DestroyRef,
    inject,
    input,
    signal,
    viewChild
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ControlContainer, ReactiveFormsModule } from '@angular/forms';

import { AutoComplete, AutoCompleteCompleteEvent, AutoCompleteModule } from 'primeng/autocomplete';

import { catchError, skip, switchMap } from 'rxjs/operators';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';

import { DotEditContentService } from '../../services/dot-edit-content.service';

export const AUTO_COMPLETE_MIN_LENGTH = 2;

export const AUTO_COMPLETE_DELAY = 300;

export const AUTO_COMPLETE_UNIQUE = true;

/**
 * Component that handles tag field input using PrimeNG's AutoComplete.
 * It provides tag suggestions as the user types with a minimum of 2 characters.
 */
@Component({
    selector: 'dot-edit-content-tag-field',
    standalone: true,
    imports: [CommonModule, AutoCompleteModule, ReactiveFormsModule],
    templateUrl: './dot-edit-content-tag-field.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    viewProviders: [
        {
            provide: ControlContainer,
            useFactory: () => inject(ControlContainer, { skipSelf: true })
        }
    ]
})
export class DotEditContentTagFieldComponent {
    #destroyRef = inject(DestroyRef);
    #editContentService = inject(DotEditContentService);
    #controlContainer = inject(ControlContainer);

    protected readonly AUTO_COMPLETE_MIN_LENGTH = AUTO_COMPLETE_MIN_LENGTH;
    protected readonly AUTO_COMPLETE_DELAY = AUTO_COMPLETE_DELAY;
    protected readonly AUTO_COMPLETE_UNIQUE = AUTO_COMPLETE_UNIQUE;

    /**
     * Required input that defines the field configuration
     */
    $field = input.required<DotCMSContentTypeField>({ alias: 'field' });

    /**
     * Reference to the PrimeNG AutoComplete component
     * Used to programmatically control the component's behavior
     */
    readonly autocomplete = viewChild<AutoComplete>('autocomplete');

    /**
     * Signal that holds the current suggestions for the autocomplete
     */
    $suggestions = signal<string[]>([]);

    /**
     * Subject that handles the search terms stream
     * Used to process user input
     */
    #searchTerms$ = new BehaviorSubject<string>('');

    constructor() {
        this.setupSearchListener();
    }

    /**
     * Sets up the search listener that handles tag suggestions.
     */
    private setupSearchListener(): void {
        this.#searchTerms$
            .pipe(
                skip(1),
                switchMap((term) => this.searchTags(term)),
                takeUntilDestroyed(this.#destroyRef)
            )
            .subscribe({
                next: (tags) => this.$suggestions.set(tags),
                error: () => this.$suggestions.set([])
            });
    }

    /**
     * Performs the actual tag search with error handling
     */
    private searchTags(term: string) {
        return this.#editContentService.getTags(term).pipe(
            catchError((error) => {
                console.error('Error fetching tags:', error);

                return EMPTY;
            })
        );
    }

    /**
     * Handles the search event from the AutoComplete component
     * Pushes the new search term into the searchTerms$ stream
     */
    onSearch(event: AutoCompleteCompleteEvent): void {
        this.#searchTerms$.next(event.query);
    }

    /**
     * Handles the Enter key press event in the tag input field.
     * Prevents form submission and allows custom values while maintaining uniqueness.
     *
     * @param {Event} event - The keyboard event object
     */
    onEnterKey(event: Event): void {
        event.preventDefault();

        const input = event.target as HTMLInputElement;
        const value = input.value.trim();

        if (value) {
            const currentValues = this.formControl?.value || [];
            const isDuplicate = currentValues.includes(value);

            if (!isDuplicate) {
                this.formControl?.setValue([...currentValues, value]);
                input.value = '';
            }
        }
    }

    /**
     * Gets the form control associated with this field
     * Used for form integration
     */
    get formControl() {
        return this.#controlContainer.control?.get(this.$field().variable);
    }
}
