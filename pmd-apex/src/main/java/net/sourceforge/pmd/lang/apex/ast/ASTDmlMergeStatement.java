/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.apex.ast;

import com.google.summit.ast.Node;

public final class ASTDmlMergeStatement extends AbstractApexNode.Single<Node> {

    ASTDmlMergeStatement(Node dmlMergeStatement) {
        super(dmlMergeStatement);
    }


    @Override
    protected <P, R> R acceptApexVisitor(ApexVisitor<? super P, ? extends R> visitor, P data) {
        return visitor.visit(this, data);
    }
}
