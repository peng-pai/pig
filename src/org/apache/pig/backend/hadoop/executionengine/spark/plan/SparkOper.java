/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.pig.backend.hadoop.executionengine.spark.plan;

import java.util.HashSet;
import java.util.Set;

import org.apache.pig.backend.hadoop.executionengine.physicalLayer.PhysicalOperator;
import org.apache.pig.backend.hadoop.executionengine.physicalLayer.plans.PhysicalPlan;
import org.apache.pig.impl.plan.Operator;
import org.apache.pig.impl.plan.OperatorKey;
import org.apache.pig.impl.plan.VisitorException;

/**
 * An operator model for a Spark job.
 * Acts as a host to the plans that will
 * execute in spark.
 */
public class SparkOper extends Operator<SparkOpPlanVisitor> {
    private static enum OPER_FEATURE {
        NONE,
        // Indicate if this job is a sampling job
        SAMPLER,
        // Indicate if this job is a merge indexer
        INDEXER,
        // Indicate if this job is a group by job
        GROUPBY,
        // Indicate if this job is a cogroup job
        COGROUP,
        // Indicate if this job is a regular join job
        HASHJOIN;
    };
    public PhysicalPlan plan;

    public Set<String> UDFs;

    /* Name of the Custom Partitioner used */
    public String customPartitioner = null;

    public Set<PhysicalOperator> scalars;

    public boolean isUDFComparatorUsed = false;

    public int requestedParallelism = -1;

    private OPER_FEATURE feature = OPER_FEATURE.NONE;

    private boolean splitter = false;

    // Name of the partition file generated by sampling process,
    // Used by Skewed Join
    private String skewedJoinPartitionFile;

    private boolean usingTypedComparator = false;

    private boolean combineSmallSplits = true;

    public SparkOper(OperatorKey k) {
        super(k);
        plan = new PhysicalPlan();
        UDFs = new HashSet<String>();
        scalars = new HashSet<PhysicalOperator>();
    }

    @Override
    public boolean supportsMultipleInputs() {
        return true;
    }

    @Override
    public boolean supportsMultipleOutputs() {
        return true;
    }

    @Override
    public String name() {
        String udfStr = getUDFsAsStr();
        StringBuilder sb = new StringBuilder("Spark" + "(" + requestedParallelism +
                (udfStr.equals("")? "" : ",") + udfStr + ")" + " - " + mKey.toString());
        return sb.toString();
    }

    private String getUDFsAsStr() {
        StringBuilder sb = new StringBuilder();
        if(UDFs!=null && UDFs.size()>0){
            for (String str : UDFs) {
                sb.append(str.substring(str.lastIndexOf('.')+1));
                sb.append(',');
            }
            sb.deleteCharAt(sb.length()-1);
        }
        return sb.toString();
    }

    public void add(PhysicalOperator physicalOper){
        this.plan.add(physicalOper);
    }

    @Override
    public void visit(SparkOpPlanVisitor v) throws VisitorException {
        v.visitSparkOp(this);
    }


    public boolean isGroupBy() {
        return (feature == OPER_FEATURE.GROUPBY);
    }

    public void markGroupBy() {
        feature = OPER_FEATURE.GROUPBY;
    }

    public boolean isCogroup() {
        return (feature == OPER_FEATURE.COGROUP);
    }

    public void markCogroup() {
        feature = OPER_FEATURE.COGROUP;
    }

    public boolean isRegularJoin() {
        return (feature == OPER_FEATURE.HASHJOIN);
    }

    public void markRegularJoin() {
        feature = OPER_FEATURE.HASHJOIN;
    }

    public int getRequestedParallelism() {
        return requestedParallelism;
    }

    public void setSplitter(boolean spl) {
        splitter = spl;
    }

    public boolean isSplitter() {
        return splitter;
    }

    public boolean isSampler() {
        return (feature == OPER_FEATURE.SAMPLER);
    }

    public void markSampler() {
        feature = OPER_FEATURE.SAMPLER;
    }

    public void setSkewedJoinPartitionFile(String file) {
        skewedJoinPartitionFile = file;
    }

    public String getSkewedJoinPartitionFile() {
        return skewedJoinPartitionFile;
    }

    protected boolean usingTypedComparator() {
        return usingTypedComparator;
    }

    protected void useTypedComparator(boolean useTypedComparator) {
        this.usingTypedComparator = useTypedComparator;
    }

    protected void noCombineSmallSplits() {
        combineSmallSplits = false;
    }

    public boolean combineSmallSplits() {
        return combineSmallSplits;
    }

    public boolean isIndexer() {
        return (feature == OPER_FEATURE.INDEXER);
    }

    public void markIndexer() {
        feature = OPER_FEATURE.INDEXER;
    }
}