import { forkJoin, Observable } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { catchError, map, pluck } from 'rxjs/operators';

import {
    DotContentSearchService,
    DotFieldService,
    DotHttpErrorManagerService,
    DotLanguagesService
} from '@dotcms/data-access';
import { DotCMSContentlet, DotCMSContentTypeField, DotLanguage } from '@dotcms/dotcms-models';

import { Column } from '../models/column.model';

type LanguagesMap = Record<number, DotLanguage>;

@Injectable({
    providedIn: 'root'
})
export class RelationshipFieldService {
    readonly #fieldService = inject(DotFieldService);
    readonly #contentSearchService = inject(DotContentSearchService);
    readonly #dotLanguagesService = inject(DotLanguagesService);
    readonly #httpErrorManagerService = inject(DotHttpErrorManagerService);

    /**
     * Gets relationship content items
     * @returns Observable of RelationshipFieldItem array
     */
    getContent(contentTypeId: string): Observable<DotCMSContentlet[]> {
        const query = `+contentType:${contentTypeId} +deleted:false +working:true`;

        return this.#contentSearchService
            .get({
                query,
                limit: 100
            })
            .pipe(pluck('jsonObjectView', 'contentlets'));
    }

    /**
     * Gets the columns and content for the relationship field
     * @param contentTypeId The content type ID
     * @returns Observable of [Column[], RelationshipFieldItem[]]
     */
    getColumnsAndContent(contentTypeId: string): Observable<[Column[], DotCMSContentlet[]] | null> {
        return forkJoin([
            this.getColumns(contentTypeId),
            this.getContent(contentTypeId),
            this.#getLanguages()
        ]).pipe(
            map(([columns, content, languages]) => [
                columns,
                this.#prepareContent(content, languages)
            ]),
            catchError((error: HttpErrorResponse) => {
                return this.#httpErrorManagerService.handle(error).pipe(map(() => null));
            })
        );
    }

    /**
     * Gets the columns for the relationship field
     * @param contentTypeId The content type ID
     * @returns Observable of Column array
     */
    getColumns(contentTypeId: string): Observable<Column[]> {
        return this.#fieldService
            .getFields(contentTypeId, 'SHOW_IN_LIST')
            .pipe(map((fields) => this.#buildColumns(fields)));
    }

    /**
     * Gets the languages for the relationship field
     * @returns Observable of Record<number, DotLanguageWithLabel>
     */
    #getLanguages(): Observable<LanguagesMap> {
        return this.#dotLanguagesService.get().pipe(
            map((languages) =>
                languages.reduce((acc, lang) => {
                    acc[lang.id] = { ...lang };

                    return acc;
                }, {})
            )
        );
    }

    /**
     * Builds the columns for the relationship field
     * @param columns The columns to build
     * @returns Array of Column
     */
    #buildColumns(columns: DotCMSContentTypeField[]): Column[] {
        return columns
            .filter((column) => column.variable && column.name)
            .map((column) => ({
                field: column.variable,
                header: column.name
            }));
    }

    /**
     * Prepares the content for the relationship field
     * @param content The contentlets to prepare
     * @param languages The languages to prepare
     * @returns Array of DotCMSContentlet
     */
    #prepareContent(content: DotCMSContentlet[], languages: LanguagesMap): DotCMSContentlet[] {
        return content.map((item) => ({
            ...item,
            title: item.title || item.identifier,
            language: languages[item.languageId]
        }));
    }
}
