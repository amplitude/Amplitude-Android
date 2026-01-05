/**
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { Context } from "semantic-release";
declare type From = FromCallback | RegExp | string;
declare type FromCallback = (filename: string, ...args: unknown[]) => RegExp | string;
declare type To = string | ToCallback;
declare type ToCallback = (match: string, ...args: unknown[]) => string;
/**
 * Replacement is simlar to the interface used by https://www.npmjs.com/package/replace-in-file
 * with the difference being the single string for `to` and `from`.
 */
export interface Replacement {
    /**
     * files to search for replacements
     */
    files: string[];
    /**
     * The RegExp pattern to use to match.
     *
     * Uses `String.replace(new RegExp(s, 'gm'), to)` for implementation, if
     * `from` is a string.
     *
     * For advanced matching, i.e. when using a `release.config.js` file, consult
     * the documentation of the `replace-in-file` package
     * (https://github.com/adamreisnz/replace-in-file/blob/main/README.md) on its
     * `from` option. This allows explicit specification of `RegExp`s, callback
     * functions, etc.
     *
     * Multiple matchers may be provided as an array, following the same
     * conversion rules as mentioned above.
     */
    from: From | From[];
    /**
     * The replacement value using a template of variables.
     *
     * `__VERSION__ = "${nextRelease.version}"`
     *
     * The context object is used to render the template. Additional values
     * can be found at: https://semantic-release.gitbook.io/semantic-release/developer-guide/js-api#result
     *
     * For advanced replacement (NOTE: only for use with `release.config.js` file version), pass in a function to replace non-standard variables
     * ```
     * {
     *    from: `__VERSION__ = 11`, // eslint-disable-line
     *    to: (matched) => `__VERSION: ${parseInt(matched.split('=')[1].trim()) + 1}`, // eslint-disable-line
     *  },
     * ```
     *
     * The `args` for a callback function can take a variety of shapes. In its
     * simplest form, e.g. if `from` is a string, it's the filename in which the
     * replacement is done. If `from` is a regular expression the `args` of the
     * callback include captures, the offset of the matched string, the matched
     * string, etc. See the `String.replace` documentation for details
     *
     * Multiple replacements may be specified as an array. These can be either
     * strings or callback functions. Note that the amount of replacements needs
     * to match the amount of `from` matchers.
     */
    to: To | To[];
    ignore?: string[];
    allowEmptyPaths?: boolean;
    countMatches?: boolean;
    disableGlobs?: boolean;
    encoding?: string;
    dry?: boolean;
    /**
     * The results array can be passed to ensure that the expected replacements
     * have been made, and if not, throw and exception with the diff.
     */
    results?: {
        file: string;
        hasChanged: boolean;
        numMatches?: number;
        numReplacements?: number;
    }[];
}
/**
 * PluginConfig is used to provide multiple replacement.
 *
 * ```
 * [
 *   "@google/semantic-release-replace-plugin",
 *   {
 *     "replacements": [
 *       {
 *         "files": ["foo/__init__.py"],
 *         "from": "__VERSION__ = \".*\"",
 *         "to": "__VERSION__ = \"${nextRelease.version}\"",
 *         "results": [
 *           {
 *             "file": "foo/__init__.py",
 *             "hasChanged": true,
 *             "numMatches": 1,
 *             "numReplacements": 1
 *           }
 *         ],
 *         "countMatches": true
 *       }
 *     ]
 *   }
 * ]
 * ```
 */
export interface PluginConfig {
    /** An array of replacements to be made. */
    replacements: Replacement[];
}
export declare function prepare(PluginConfig: PluginConfig, context: Context): Promise<void>;
export {};
