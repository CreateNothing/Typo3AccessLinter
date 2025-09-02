package com.typo3.fluid.linter.fixes;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiRecursiveElementVisitor;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Quick fix: generate a basic <thead>/<th> scaffold for data tables.
 */
public class GenerateTableHeaderScaffoldFixStrategy implements FixStrategy {
    @Override
    public LocalQuickFix createFix(PsiFile file, int startOffset, int endOffset, FixContext context) {
        return new GenerateTableHeadersFix();
    }

    @Override
    public boolean canHandle(String problemType) {
        return "table-headers-missing".equals(problemType);
    }

    private static class GenerateTableHeadersFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() { return "Generate <thead> with <th> headers"; }

        @NotNull
        @Override
        public String getFamilyName() { return "Tables"; }

        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            PsiElement element = descriptor.getPsiElement();
            XmlTag table = findTable(element);
            if (table == null) return;

            int columns = inferColumnCount(table);
            if (columns <= 0) columns = 1;

            StringBuilder sb = new StringBuilder();
            sb.append("<thead><tr>");
            for (int i = 1; i <= columns; i++) {
                sb.append("<th scope=\"col\">Header ").append(i).append("</th>");
            }
            sb.append("</tr></thead>");

            PsiMutationUtils.insertAfterStartTag(project, table.getContainingFile(), table, sb.toString());
        }

        private XmlTag findTable(PsiElement el) {
            PsiElement cur = el;
            while (cur != null) {
                if (cur instanceof XmlTag) {
                    XmlTag t = (XmlTag) cur;
                    if ("table".equalsIgnoreCase(t.getName())) return t;
                }
                cur = cur.getParent();
            }
            return null;
        }

        private int inferColumnCount(XmlTag table) {
            AtomicInteger cols = new AtomicInteger(0);
            // Find first <tr> and count its <td> or <th>
            table.accept(new PsiRecursiveElementVisitor() {
                boolean firstTrSeen = false;
                @Override
                public void visitElement(@NotNull PsiElement element) {
                    if (firstTrSeen) return;
                    if (element instanceof XmlTag) {
                        XmlTag tag = (XmlTag) element;
                        String n = tag.getName().toLowerCase();
                        if (n.equals("tr")) {
                            firstTrSeen = true;
                            int c = 0;
                            for (XmlTag child : tag.getSubTags()) {
                                String cn = child.getName().toLowerCase();
                                if (cn.equals("td") || cn.equals("th")) c++;
                            }
                            cols.set(c);
                            return;
                        }
                    }
                    super.visitElement(element);
                }
            });
            return cols.get();
        }
    }
}
