"use strict";
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
var __assign = (this && this.__assign) || function () {
    __assign = Object.assign || function(t) {
        for (var s, i = 1, n = arguments.length; i < n; i++) {
            s = arguments[i];
            for (var p in s) if (Object.prototype.hasOwnProperty.call(s, p))
                t[p] = s[p];
        }
        return t;
    };
    return __assign.apply(this, arguments);
};
var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    function adopt(value) { return value instanceof P ? value : new P(function (resolve) { resolve(value); }); }
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : adopt(result.value).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
var __generator = (this && this.__generator) || function (thisArg, body) {
    var _ = { label: 0, sent: function() { if (t[0] & 1) throw t[1]; return t[1]; }, trys: [], ops: [] }, f, y, t, g;
    return g = { next: verb(0), "throw": verb(1), "return": verb(2) }, typeof Symbol === "function" && (g[Symbol.iterator] = function() { return this; }), g;
    function verb(n) { return function (v) { return step([n, v]); }; }
    function step(op) {
        if (f) throw new TypeError("Generator is already executing.");
        while (_) try {
            if (f = 1, y && (t = op[0] & 2 ? y["return"] : op[0] ? y["throw"] || ((t = y["return"]) && t.call(y), 0) : y.next) && !(t = t.call(y, op[1])).done) return t;
            if (y = 0, t) op = [op[0] & 2, t.value];
            switch (op[0]) {
                case 0: case 1: t = op; break;
                case 4: _.label++; return { value: op[1], done: false };
                case 5: _.label++; y = op[1]; op = [0]; continue;
                case 7: op = _.ops.pop(); _.trys.pop(); continue;
                default:
                    if (!(t = _.trys, t = t.length > 0 && t[t.length - 1]) && (op[0] === 6 || op[0] === 2)) { _ = 0; continue; }
                    if (op[0] === 3 && (!t || (op[1] > t[0] && op[1] < t[3]))) { _.label = op[1]; break; }
                    if (op[0] === 6 && _.label < t[1]) { _.label = t[1]; t = op; break; }
                    if (t && _.label < t[2]) { _.label = t[2]; _.ops.push(op); break; }
                    if (t[2]) _.ops.pop();
                    _.trys.pop(); continue;
            }
            op = body.call(thisArg, _);
        } catch (e) { op = [6, e]; y = 0; } finally { f = t = 0; }
        if (op[0] & 5) throw op[1]; return { value: op[0] ? op[1] : void 0, done: true };
    }
};
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
exports.__esModule = true;
exports.prepare = void 0;
var replace_in_file_1 = require("replace-in-file");
var lodash_1 = require("lodash");
var jest_diff_1 = __importDefault(require("jest-diff"));
/**
 * Wraps the `callback` in a new function that passes the `context` as the
 * final argument to the `callback` when it gets called.
 */
function applyContextToCallback(callback, context) {
    return function () {
        var args = [];
        for (var _i = 0; _i < arguments.length; _i++) {
            args[_i] = arguments[_i];
        }
        return callback.apply(null, args.concat(context));
    };
}
/**
 * Applies the `context` to the replacement property `to` depending on whether
 * it is a string template or a callback function.
 */
function applyContextToReplacement(to, context) {
    return typeof to === "function"
        ? applyContextToCallback(to, context)
        : lodash_1.template(to)(__assign({}, context));
}
/**
 * Normalizes a `value` into an array, making it more straightforward to apply
 * logic to a single value of type `T` or an array of those values.
 */
function normalizeToArray(value) {
    return value instanceof Array ? value : [value];
}
function prepare(PluginConfig, context) {
    return __awaiter(this, void 0, void 0, function () {
        var _i, _a, replacement, results, replaceInFileConfig, actual;
        return __generator(this, function (_b) {
            switch (_b.label) {
                case 0:
                    _i = 0, _a = PluginConfig.replacements;
                    _b.label = 1;
                case 1:
                    if (!(_i < _a.length)) return [3 /*break*/, 4];
                    replacement = _a[_i];
                    results = replacement.results;
                    delete replacement.results;
                    replaceInFileConfig = replacement;
                    // The `replace-in-file` package uses `String.replace` under the hood for
                    // the actual replacement. If `from` is a string, this means only a
                    // single occurence will be replaced. This plugin intents to replace
                    // _all_ occurrences when given a string to better support
                    // configuration through JSON, this requires conversion into a `RegExp`.
                    //
                    // If `from` is a callback function, the `context` is passed as the final
                    // parameter to the function. In all other cases, e.g. being a
                    // `RegExp`, the `from` property does not require any modifications.
                    //
                    // The `from` property may either be a single value to match or an array of
                    // values (in any of the previously described forms)
                    replaceInFileConfig.from = normalizeToArray(replacement.from).map(function (from) {
                        switch (typeof from) {
                            case "function":
                                return applyContextToCallback(from, context);
                            case "string":
                                return new RegExp(from, "gm");
                            default:
                                return from;
                        }
                    });
                    replaceInFileConfig.to =
                        replacement.to instanceof Array
                            ? replacement.to.map(function (to) { return applyContextToReplacement(to, context); })
                            : applyContextToReplacement(replacement.to, context);
                    return [4 /*yield*/, replace_in_file_1.replaceInFile(replaceInFileConfig)];
                case 2:
                    actual = _b.sent();
                    if (results) {
                        results = results.sort();
                        actual = actual.sort();
                        if (!lodash_1.isEqual(actual.sort(), results.sort())) {
                            throw new Error("Expected match not found!\n" + jest_diff_1["default"](actual, results));
                        }
                    }
                    _b.label = 3;
                case 3:
                    _i++;
                    return [3 /*break*/, 1];
                case 4: return [2 /*return*/];
            }
        });
    });
}
exports.prepare = prepare;
