package org.hsqldb;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Scanner;

import org.hsqldb.HsqlNameManager.HsqlName;
import org.hsqldb.HsqlNameManager.SimpleName;
import org.hsqldb.error.Error;
import org.hsqldb.error.ErrorCode;
import org.hsqldb.jdbc.JDBCResultSet;
import org.hsqldb.lib.HashMappedList;
import org.hsqldb.lib.HsqlArrayList;
import org.hsqldb.lib.OrderedHashSet;
import org.hsqldb.result.Result;
import org.hsqldb.rights.Grantee;
import org.hsqldb.store.BitMap;
import org.hsqldb.types.RowType;
import org.hsqldb.types.Type;
import org.hsqldb.types.Types;

public class RoutineManager {
    private Routine routine;
    private boolean[] hasConnection;
    private boolean returnsTable;

    public RoutineManager(Routine routine, boolean[] hasConnection, boolean returnsTable) {
        this.routine = routine;
        this.hasConnection = hasConnection;
        this.returnsTable = returnsTable;
    }

    public boolean hasConnection(Method method) {
        Class[] params = method.getParameterTypes();
        if (params.length > 0 && params[0].equals(java.sql.Connection.class)) {
            hasConnection[0] = true;
            return true;
        }
        return false;
    }

    public boolean isValidParameterCount(Method method, int offset) {
        return method.getParameterTypes().length - offset == routine.parameterTypes.length;
    }

    public boolean isValidReturnType(Method method) {
        if (returnsTable) {
            return java.sql.ResultSet.class.isAssignableFrom(method.getReturnType());
        } else {
            Type methodReturnType = Types.getParameterSQLType(method.getReturnType());
            return methodReturnType != null && methodReturnType.typeCode == routine.returnType.typeCode;
        }
    }

    public boolean isValidParameterTypes(Method method, int offset) {
        for (int j = 0; j < routine.parameterTypes.length; j++) {
            Class param = method.getParameterTypes()[j + offset];
            boolean isInOut = param.isArray() && !byte[].class.equals(param);
            if (isInOut) {
                param = param.getComponentType();
            }

            Type methodParamType = Types.getParameterSQLType(param);
            if (methodParamType == null) {
                return false;
            }

            boolean result = routine.parameterTypes[j].typeComparisonGroup == methodParamType.typeComparisonGroup;
            if (result && routine.parameterTypes[j].isNumberType()) {
                result = routine.parameterTypes[j].typeCode == methodParamType.typeCode;
            }

            if (isInOut && routine.getParameter(j).parameterMode == SchemaObject.ParameterModes.PARAM_IN) {
                result = false;
            }

            if (!result) {
                return false;
            }
        }
        return true;
    }

    public void setParameterNullability(Method method, int offset) {
        for (int j = 0; j < routine.parameterTypes.length; j++) {
            routine.getParameter(j).setNullable(!method.getParameterTypes()[j + offset].isPrimitive());
        }
    }
}
