/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.configuration.ui

import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.NonEmptyInputValidator
import com.intellij.ui.AddEditRemovePanel
import org.jetbrains.kotlin.name.FqNameUnsafe

class NotPropertyListPanel(data: List<FqNameUnsafe>) :
        AddEditRemovePanel<FqNameUnsafe>(MyTableModel(), data, "Excluded methods") {

    var modified = false

    public override fun removeItem(fqName: FqNameUnsafe): Boolean {
        return true
    }

    public override fun editItem(fqName: FqNameUnsafe): FqNameUnsafe? {

        val result = Messages.showInputDialog(this, "Enter fully-qualified method name:",
                                              "Edit exclusion",
                                              Messages.getQuestionIcon(),
                                              fqName.asString(),
                                              NonEmptyInputValidator()
        ) ?: return null

        val created = FqNameUnsafe(result)
        if (data.contains(created)) {
            return null
        }
        modified = true
        return created
    }

    public override fun addItem(): FqNameUnsafe? {
        val result = Messages.showInputDialog(this, "Enter fully-qualified method name:",
                                              "Add exclusion",
                                              Messages.getQuestionIcon(),
                                              "",
                                              NonEmptyInputValidator()
        ) ?: return null

        val created = FqNameUnsafe(result)
        if (data.contains(created)) {
            return null
        }
        modified = true
        return created
    }

    private class MyTableModel : AddEditRemovePanel.TableModel<FqNameUnsafe>() {
        override fun getField(o: FqNameUnsafe, columnIndex: Int): Any {
            return o.asString()
        }

        override fun getColumnName(columnIndex: Int): String? {
            return "Method"
        }

        override fun getColumnCount(): Int {
            return 1
        }
    }
}

