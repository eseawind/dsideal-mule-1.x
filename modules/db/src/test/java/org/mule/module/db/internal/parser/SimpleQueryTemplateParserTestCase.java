/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.parser;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.mule.module.db.internal.domain.param.InputQueryParam;
import org.mule.module.db.internal.domain.param.QueryParam;
import org.mule.module.db.internal.domain.query.QueryTemplate;
import org.mule.module.db.internal.domain.query.QueryType;
import org.mule.module.db.internal.domain.type.UnknownDbType;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

@SmallTest
public class SimpleQueryTemplateParserTestCase extends AbstractMuleTestCase
{

    private final SimpleQueryTemplateParser parser = new SimpleQueryTemplateParser();

    @Test
    public void detectsSelect() throws Exception
    {
        String sql = "SELECT * FROM PLANET";
        QueryTemplate queryTemplate = parser.parse(sql);
        assertEquals(QueryType.SELECT, queryTemplate.getType());
        assertEquals(sql, queryTemplate.getSqlText());
        assertEquals(0, queryTemplate.getInputParams().size());
    }

    @Test
    public void detectsSelectWithLineBreak() throws Exception
    {
        String sql = "SELECT *\nFROM PLANET";
        QueryTemplate queryTemplate = parser.parse(sql);
        assertEquals(QueryType.SELECT, queryTemplate.getType());
        assertEquals(sql, queryTemplate.getSqlText());
        assertEquals(0, queryTemplate.getInputParams().size());
    }

    @Test
    public void detectsUpdate() throws Exception
    {
        String sql = "update PLANET set NAME='Mercury' where ID=1";
        QueryTemplate queryTemplate = parser.parse(sql);
        assertEquals(QueryType.UPDATE, queryTemplate.getType());
        assertEquals(sql, queryTemplate.getSqlText());
        assertEquals(0, queryTemplate.getInputParams().size());
    }

    @Test
    public void detectsUpdateWithLineBreak() throws Exception
    {
        String sql = "update PLANET set NAME='Mercury' \nwhere ID=1";
        QueryTemplate queryTemplate = parser.parse(sql);
        assertEquals(QueryType.UPDATE, queryTemplate.getType());
        assertEquals(sql, queryTemplate.getSqlText());
        assertEquals(0, queryTemplate.getInputParams().size());
    }

    @Test
    public void detectsInsert() throws Exception
    {
        String sql = "INSERT INTO PLANET(POSITION, NAME) VALUES (777, 'Mercury')";
        QueryTemplate queryTemplate = parser.parse(sql);
        assertEquals(QueryType.INSERT, queryTemplate.getType());
        assertEquals(sql, queryTemplate.getSqlText());
        assertEquals(0, queryTemplate.getInputParams().size());
    }

    @Test
    public void detectsInsertWithLineBreak() throws Exception
    {
        String sql = "INSERT INTO PLANET(\nPOSITION, NAME) VALUES (777, 'Mercury')";
        QueryTemplate queryTemplate = parser.parse(sql);
        assertEquals(QueryType.INSERT, queryTemplate.getType());
        assertEquals(sql, queryTemplate.getSqlText());
        assertEquals(0, queryTemplate.getInputParams().size());
    }

    @Test
    public void detectsDelete() throws Exception
    {
        String sql = "delete from PLANET";
        QueryTemplate queryTemplate = parser.parse(sql);
        assertEquals(QueryType.DELETE, queryTemplate.getType());
        assertEquals(sql, queryTemplate.getSqlText());
        assertEquals(0, queryTemplate.getInputParams().size());
    }

    @Test
    public void detectsDeleteWithLineBreak() throws Exception
    {
        String sql = "delete from PLANET";
        QueryTemplate queryTemplate = parser.parse(sql);
        assertEquals(QueryType.DELETE, queryTemplate.getType());
        assertEquals(sql, queryTemplate.getSqlText());
        assertEquals(0, queryTemplate.getInputParams().size());
    }

    @Test
    public void detectsDdl() throws Exception
    {
        String sql = "drop table PLANET";

        QueryTemplate queryTemplate = parser.parse(sql);

        assertThat(queryTemplate.getType(), equalTo(QueryType.DDL));
        assertThat(queryTemplate.getSqlText(), equalTo(sql));
        assertThat(queryTemplate.getParams().size(), equalTo(0));
    }

    @Test
    public void detectsMerge() throws Exception
    {
        String sql = "merge into PLANET USING ALIEN";
        QueryTemplate queryTemplate = parser.parse(sql);
        assertEquals(QueryType.MERGE, queryTemplate.getType());
        assertEquals(sql, queryTemplate.getSqlText());
        assertEquals(0, queryTemplate.getInputParams().size());
    }

    @Test
    public void detectsMergeWithLineBreak() throws Exception
    {
        String sql = "merge into PLANET\n USING ALIEN";
        QueryTemplate queryTemplate = parser.parse(sql);
        assertEquals(QueryType.MERGE, queryTemplate.getType());
        assertEquals(sql, queryTemplate.getSqlText());
        assertEquals(0, queryTemplate.getInputParams().size());
    }

    @Test
    public void detectsStoredProcedureWithBrackets() throws Exception
    {
        doStoredProcedureParsingTest("{ call getTestRecords() }");
    }

    @Test
    public void detectsStoredProcedureWithOutBrackets() throws Exception
    {
        doStoredProcedureParsingTest("call getTestRecords()");
    }

    @Test
    public void detectsStoredProcedureWithSpaces() throws Exception
    {
        doStoredProcedureParsingTest("  {  call  getTestRecords()  } ");
    }

    @Test
    public void detectsStoredProcedureWithLineBreak() throws Exception
    {
        doStoredProcedureParsingTest("{ call \ngetTestRecords() } ");
    }

    @Test
    public void detectsStoredProcedureAssignment() throws Exception
    {
        String sql = "{ :out = call getTestRecords() } ";

        QueryTemplate queryTemplate = parser.parse(sql);

        assertThat(queryTemplate.getType(), equalTo(QueryType.STORE_PROCEDURE_CALL));
        assertThat(queryTemplate.getSqlText(), equalTo("{ ? = call getTestRecords() }"));
        assertThat(queryTemplate.getParams().size(), equalTo(1));

        QueryParam param1 = queryTemplate.getParams().get(0);
        assertThat(param1.getName(), equalTo("out"));
        assertThat(param1.getType(), equalTo(UnknownDbType.getInstance()));
    }

    @Test
    public void detectsMissingOutParamStoredProcedureAssignmentAsDdl() throws Exception
    {

        doIncompleteStoredProcedureAssingmentTest("{  = call getTestRecords() } ");
    }

    @Test
    public void detectsMissingParamNameStoredProcedureAssignmentAsDdl() throws Exception
    {
        doIncompleteStoredProcedureAssingmentTest("{ : = call getTestRecords() } ");
    }

    @Test
    public void detectsMissingParamEscapingStoredProcedureAssignmentAsDdl() throws Exception
    {
        doIncompleteStoredProcedureAssingmentTest("{ a = call getTestRecords() } ");
    }

    private void doIncompleteStoredProcedureAssingmentTest(String sql)
    {
        QueryTemplate queryTemplate = parser.parse(sql);
        assertThat(queryTemplate.getType(), equalTo(QueryType.DDL));
    }

    @Test
    public void detectsStoredProcedureWithoutSpaceAfterBracket() throws Exception
    {
        doStoredProcedureParsingTest("{call getTestRecords() } ");
    }

    private void doStoredProcedureParsingTest(String sql)
    {
        QueryTemplate queryTemplate = parser.parse(sql);
        assertEquals(QueryType.STORE_PROCEDURE_CALL, queryTemplate.getType());
        assertEquals(sql.trim(), queryTemplate.getSqlText());
        assertEquals(0, queryTemplate.getInputParams().size());
    }

    @Test
    public void parsesQuestionMarkParam() throws Exception
    {
        String sql = "SELECT * FROM PLANET WHERE POSITION = ?";
        QueryTemplate queryTemplate = parser.parse(sql);
        assertEquals(QueryType.SELECT, queryTemplate.getType());
        assertEquals(sql, queryTemplate.getSqlText());
        assertEquals(1, queryTemplate.getInputParams().size());
        InputQueryParam param1 = queryTemplate.getInputParams().get(0);
        assertEquals(UnknownDbType.getInstance(), param1.getType());
    }

    @Test
    public void parsesNamedParam() throws Exception
    {
        QueryTemplate queryTemplate = parser.parse("update PLANET set NAME='Mercury' where ID= :planetId");
        assertEquals(QueryType.UPDATE, queryTemplate.getType());
        assertEquals("update PLANET set NAME='Mercury' where ID= ?", queryTemplate.getSqlText());
        assertEquals(1, queryTemplate.getInputParams().size());
        InputQueryParam param1 = queryTemplate.getInputParams().get(0);
        assertEquals(UnknownDbType.getInstance(), param1.getType());
        assertEquals("planetId", param1.getName());
        assertNull(param1.getValue());
    }

    @Test
    public void definesTemplateWithExpressionParam() throws Exception
    {
        QueryTemplate queryTemplate = parser.parse("update PLANET set NAME='Mercury' where ID= #[planetId]");

        assertEquals(QueryType.UPDATE, queryTemplate.getType());
        assertEquals("update PLANET set NAME='Mercury' where ID= ?", queryTemplate.getSqlText());
        assertEquals(1, queryTemplate.getInputParams().size());
        InputQueryParam param1 = queryTemplate.getInputParams().get(0);
        assertEquals(UnknownDbType.getInstance(), param1.getType());
        assertNull(param1.getName());
        assertEquals("#[planetId]", param1.getValue());
    }

    @Test
    public void definesTemplateWithComplexExpressionParam() throws Exception
    {
        QueryTemplate queryTemplate = parser.parse("SELECT * FROM PLANET WHERE POSITION = #[message.inboundProperties['position']] AND NAME= #[planetName]");

        assertEquals(QueryType.SELECT, queryTemplate.getType());
        assertEquals("SELECT * FROM PLANET WHERE POSITION = ? AND NAME= ?", queryTemplate.getSqlText());
        assertEquals(2, queryTemplate.getInputParams().size());
        InputQueryParam param1 = queryTemplate.getInputParams().get(0);
        assertEquals(UnknownDbType.getInstance(), param1.getType());
        assertNull(param1.getName());
        assertEquals("#[message.inboundProperties['position']]", param1.getValue());

        InputQueryParam param2 = queryTemplate.getInputParams().get(1);
        assertEquals(UnknownDbType.getInstance(), param2.getType());
        assertNull(param2.getName());
        assertEquals("#[planetName]", param2.getValue());
    }

    @Test
    public void detectsUnterminatedMuleExpression() throws Exception
    {
        try
        {
            parser.parse("SELECT * FROM PLANET where id = #[incompleteExpression");
            fail("Did not detect an unfinished mule expression");
        }
        catch (QueryTemplateParsingException e)
        {
            assertTrue("Error message did not contains invalid expression", e.getMessage().endsWith("#[incompleteExpression"));
        }
    }
}
