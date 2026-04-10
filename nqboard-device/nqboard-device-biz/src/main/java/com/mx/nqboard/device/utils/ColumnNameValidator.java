package com.mx.nqboard.device.utils;

import com.mx.nqboard.common.core.exception.ValidateCodeException;
import com.mx.nqboard.common.core.util.MsgUtils;
import com.mx.nqboard.device.api.constant.IotProductConstant;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 校验字段是否符合规范
 *
 * @author 泥鳅压滑板
 */
public class ColumnNameValidator {
    /**
     * 定义数据库保留关键字集合
     */
    private static final Set<String> RESERVED_KEYWORDS = new HashSet<>();

    static {
        // 初始化数据库保留关键字
        RESERVED_KEYWORDS.add("SELECT");
        RESERVED_KEYWORDS.add("INSERT");
        RESERVED_KEYWORDS.add("DELETE");
        RESERVED_KEYWORDS.add("UPDATE");
        RESERVED_KEYWORDS.add("FROM");
        RESERVED_KEYWORDS.add("WHERE");
        RESERVED_KEYWORDS.add("TABLE");
        RESERVED_KEYWORDS.add("JOIN");
        RESERVED_KEYWORDS.add("GROUP");
        RESERVED_KEYWORDS.add("ORDER");
        RESERVED_KEYWORDS.add("BY");
        RESERVED_KEYWORDS.add("STABLE");
        RESERVED_KEYWORDS.add("ABORT");
        RESERVED_KEYWORDS.add("ACCOUNT");
        RESERVED_KEYWORDS.add("ACCOUNTS");
        RESERVED_KEYWORDS.add("ADD");
        RESERVED_KEYWORDS.add("AFTER");
        RESERVED_KEYWORDS.add("AGGREGATE");
        RESERVED_KEYWORDS.add("ALIAS");
        RESERVED_KEYWORDS.add("ALIVE");
        RESERVED_KEYWORDS.add("ALL");
        RESERVED_KEYWORDS.add("ALTER");
        RESERVED_KEYWORDS.add("ANALYZE");
        RESERVED_KEYWORDS.add("AND");
        RESERVED_KEYWORDS.add("ANODE");
        RESERVED_KEYWORDS.add("ANODES");
        RESERVED_KEYWORDS.add("ANOMALY_WINDOW");
        RESERVED_KEYWORDS.add("ANTI");
        RESERVED_KEYWORDS.add("APPS");
        RESERVED_KEYWORDS.add("ARBGROUPS");
        RESERVED_KEYWORDS.add("ARROW");
        RESERVED_KEYWORDS.add("AS");
        RESERVED_KEYWORDS.add("ASC");
        RESERVED_KEYWORDS.add("ASOF");
        RESERVED_KEYWORDS.add("ASYNC");
        RESERVED_KEYWORDS.add("AT_ONCE");
        RESERVED_KEYWORDS.add("ATTACH");
        RESERVED_KEYWORDS.add("AUTO");
        RESERVED_KEYWORDS.add("ASSIGN");
        RESERVED_KEYWORDS.add("BALANCE");
        RESERVED_KEYWORDS.add("BEFORE");
        RESERVED_KEYWORDS.add("BEGIN");
        RESERVED_KEYWORDS.add("BETWEEN");
        RESERVED_KEYWORDS.add("BIGINT");
        RESERVED_KEYWORDS.add("BIN");
        RESERVED_KEYWORDS.add("BINARY");
        RESERVED_KEYWORDS.add("BITAND");
        RESERVED_KEYWORDS.add("BITNOT");
        RESERVED_KEYWORDS.add("BITOR");
        RESERVED_KEYWORDS.add("BLOB");
        RESERVED_KEYWORDS.add("BLOCKS");
        RESERVED_KEYWORDS.add("BNODE");
        RESERVED_KEYWORDS.add("BNODES");
        RESERVED_KEYWORDS.add("BOOL");
        RESERVED_KEYWORDS.add("BOTH");
        RESERVED_KEYWORDS.add("BUFFER");
        RESERVED_KEYWORDS.add("BUFSIZE");
        RESERVED_KEYWORDS.add("BWLIMIT");
        RESERVED_KEYWORDS.add("CACHE");
        RESERVED_KEYWORDS.add("CACHEMODEL");
        RESERVED_KEYWORDS.add("CACHESIZE");
        RESERVED_KEYWORDS.add("CASE");
        RESERVED_KEYWORDS.add("CAST");
        RESERVED_KEYWORDS.add("CHANGE");
        RESERVED_KEYWORDS.add("CHILD");
        RESERVED_KEYWORDS.add("CLIENT_VERSION");
        RESERVED_KEYWORDS.add("CLUSTER");
        RESERVED_KEYWORDS.add("COLON");
        RESERVED_KEYWORDS.add("COLS");
        RESERVED_KEYWORDS.add("COLUMN");
        RESERVED_KEYWORDS.add("COMMA");
        RESERVED_KEYWORDS.add("COMMENT");
        RESERVED_KEYWORDS.add("COMP");
        RESERVED_KEYWORDS.add("COMPACT");
        RESERVED_KEYWORDS.add("COMPACTS");
        RESERVED_KEYWORDS.add("COMPACT_INTERVAL");
        RESERVED_KEYWORDS.add("COMPACT_TIME_OFFSET");
        RESERVED_KEYWORDS.add("COMPACT_TIME_RANGE");
        RESERVED_KEYWORDS.add("CONCAT");
        RESERVED_KEYWORDS.add("CONFLICT");
        RESERVED_KEYWORDS.add("CONNECTION");
        RESERVED_KEYWORDS.add("CONNECTIONS");
        RESERVED_KEYWORDS.add("CONNS");
        RESERVED_KEYWORDS.add("CONSUMER");
        RESERVED_KEYWORDS.add("CONSUMERS");
        RESERVED_KEYWORDS.add("CONTAINS");
        RESERVED_KEYWORDS.add("CONTINUOUS_WINDOW_CLOSE");
        RESERVED_KEYWORDS.add("COPY");
        RESERVED_KEYWORDS.add("COUNT");
        RESERVED_KEYWORDS.add("COUNT_WINDOW");
        RESERVED_KEYWORDS.add("CREATE");
        RESERVED_KEYWORDS.add("CREATEDB");
        RESERVED_KEYWORDS.add("CURRENT_USER");
        RESERVED_KEYWORDS.add("DATABASE");
        RESERVED_KEYWORDS.add("DATABASES");
        RESERVED_KEYWORDS.add("DBS");
        RESERVED_KEYWORDS.add("DECIMAL");
        RESERVED_KEYWORDS.add("DEFERRED");
        RESERVED_KEYWORDS.add("DELETE_MARK");
        RESERVED_KEYWORDS.add("DELIMITERS");
        RESERVED_KEYWORDS.add("DESC");
        RESERVED_KEYWORDS.add("DESCRIBE");
        RESERVED_KEYWORDS.add("DETACH");
        RESERVED_KEYWORDS.add("DISK_INFO");
        RESERVED_KEYWORDS.add("DISTINCT");
        RESERVED_KEYWORDS.add("DISTRIBUTED");
        RESERVED_KEYWORDS.add("DIVIDE");
        RESERVED_KEYWORDS.add("DNODE");
        RESERVED_KEYWORDS.add("DNODES");
        RESERVED_KEYWORDS.add("DOT");
        RESERVED_KEYWORDS.add("DOUBLE");
        RESERVED_KEYWORDS.add("DROP");
        RESERVED_KEYWORDS.add("DURATION");
        RESERVED_KEYWORDS.add("EACH");
        RESERVED_KEYWORDS.add("ELSE");
        RESERVED_KEYWORDS.add("ENABLE");
        RESERVED_KEYWORDS.add("ENCRYPT_ALGORITHM");
        RESERVED_KEYWORDS.add("ENCRYPT_KEY");
        RESERVED_KEYWORDS.add("ENCRYPTIONS");
        RESERVED_KEYWORDS.add("END");
        RESERVED_KEYWORDS.add("EQ");
        RESERVED_KEYWORDS.add("EVENT_WINDOW");
        RESERVED_KEYWORDS.add("EVERY");
        RESERVED_KEYWORDS.add("EXCEPT");
        RESERVED_KEYWORDS.add("EXISTS");
        RESERVED_KEYWORDS.add("EXPIRED");
        RESERVED_KEYWORDS.add("EXPLAIN");
        RESERVED_KEYWORDS.add("FAIL");
        RESERVED_KEYWORDS.add("FHIGH");
        RESERVED_KEYWORDS.add("FILE");
        RESERVED_KEYWORDS.add("FILL");
        RESERVED_KEYWORDS.add("FILL_HISTORY");
        RESERVED_KEYWORDS.add("FIRST");
        RESERVED_KEYWORDS.add("FLOAT");
        RESERVED_KEYWORDS.add("FLOW");
        RESERVED_KEYWORDS.add("FLUSH");
        RESERVED_KEYWORDS.add("FOR");
        RESERVED_KEYWORDS.add("FORCE");
        RESERVED_KEYWORDS.add("FORCE_WINDOW_CLOSE");
        RESERVED_KEYWORDS.add("FROWTS");
        RESERVED_KEYWORDS.add("FULL");
        RESERVED_KEYWORDS.add("FUNCTION");
        RESERVED_KEYWORDS.add("FUNCTIONS");
        RESERVED_KEYWORDS.add("GE");
        RESERVED_KEYWORDS.add("GEOMETRY");
        RESERVED_KEYWORDS.add("GLOB");
        RESERVED_KEYWORDS.add("GRANT");
        RESERVED_KEYWORDS.add("GRANTS");
        RESERVED_KEYWORDS.add("GT");
        RESERVED_KEYWORDS.add("HAVING");
        RESERVED_KEYWORDS.add("HEX");
        RESERVED_KEYWORDS.add("HOST");
        RESERVED_KEYWORDS.add("ID");
        RESERVED_KEYWORDS.add("IF");
        RESERVED_KEYWORDS.add("IGNORE");
        RESERVED_KEYWORDS.add("ILLEGAL");
        RESERVED_KEYWORDS.add("IMMEDIATE");
        RESERVED_KEYWORDS.add("IMPORT");
        RESERVED_KEYWORDS.add("IN");
        RESERVED_KEYWORDS.add("INDEX");
        RESERVED_KEYWORDS.add("INDEXES");
        RESERVED_KEYWORDS.add("INITIALLY");
        RESERVED_KEYWORDS.add("INNER");
        RESERVED_KEYWORDS.add("INSTEAD");
        RESERVED_KEYWORDS.add("INT");
        RESERVED_KEYWORDS.add("INTEGER");
        RESERVED_KEYWORDS.add("INTERSECT");
        RESERVED_KEYWORDS.add("INTERVAL");
        RESERVED_KEYWORDS.add("INTO");
        RESERVED_KEYWORDS.add("IPTOKEN");
        RESERVED_KEYWORDS.add("IROWTS");
        RESERVED_KEYWORDS.add("IROWTS_ORIGIN");
        RESERVED_KEYWORDS.add("IS");
        RESERVED_KEYWORDS.add("IS_IMPORT");
        RESERVED_KEYWORDS.add("ISFILLED");
        RESERVED_KEYWORDS.add("ISNULL");
        RESERVED_KEYWORDS.add("JLIMIT");
        RESERVED_KEYWORDS.add("JSON");
        RESERVED_KEYWORDS.add("KEEP");
        RESERVED_KEYWORDS.add("KEEP_TIME_OFFSET");
        RESERVED_KEYWORDS.add("KEY");
        RESERVED_KEYWORDS.add("KILL");
        RESERVED_KEYWORDS.add("LANGUAGE");
        RESERVED_KEYWORDS.add("LAST");
        RESERVED_KEYWORDS.add("LAST_ROW");
        RESERVED_KEYWORDS.add("LE");
        RESERVED_KEYWORDS.add("LEADER");
        RESERVED_KEYWORDS.add("LEADING");
        RESERVED_KEYWORDS.add("LEFT");
        RESERVED_KEYWORDS.add("LEVEL");
        RESERVED_KEYWORDS.add("LICENCES");
        RESERVED_KEYWORDS.add("LIKE");
        RESERVED_KEYWORDS.add("LIMIT");
        RESERVED_KEYWORDS.add("LINEAR");
        RESERVED_KEYWORDS.add("LOCAL");
        RESERVED_KEYWORDS.add("LOGS");
        RESERVED_KEYWORDS.add("LP");
        RESERVED_KEYWORDS.add("LSHIFT");
        RESERVED_KEYWORDS.add("LT");
        RESERVED_KEYWORDS.add("MACHINES");
        RESERVED_KEYWORDS.add("MATCH");
        RESERVED_KEYWORDS.add("MAX_DELAY");
        RESERVED_KEYWORDS.add("MAXROWS");
        RESERVED_KEYWORDS.add("MEDIUMBLOB");
        RESERVED_KEYWORDS.add("MERGE");
        RESERVED_KEYWORDS.add("META");
        RESERVED_KEYWORDS.add("META_ONLY");
        RESERVED_KEYWORDS.add("MINROWS");
        RESERVED_KEYWORDS.add("MINUS");
        RESERVED_KEYWORDS.add("MNODE");
        RESERVED_KEYWORDS.add("MNODES");
        RESERVED_KEYWORDS.add("MODIFY");
        RESERVED_KEYWORDS.add("MODULES");
        RESERVED_KEYWORDS.add("NCHAR");
        RESERVED_KEYWORDS.add("NE");
        RESERVED_KEYWORDS.add("NEXT");
        RESERVED_KEYWORDS.add("NMATCH");
        RESERVED_KEYWORDS.add("NONE");
        RESERVED_KEYWORDS.add("NORMAL");
        RESERVED_KEYWORDS.add("NOT");
        RESERVED_KEYWORDS.add("NOTIFY");
        RESERVED_KEYWORDS.add("NOTIFY_HISTORY");
        RESERVED_KEYWORDS.add("NOTNULL");
        RESERVED_KEYWORDS.add("NOW");
        RESERVED_KEYWORDS.add("NULL");
        RESERVED_KEYWORDS.add("NULL_F");
        RESERVED_KEYWORDS.add("NULLS");
        RESERVED_KEYWORDS.add("OF");
        RESERVED_KEYWORDS.add("OFFSET");
        RESERVED_KEYWORDS.add("ON");
        RESERVED_KEYWORDS.add("ONLY");
        RESERVED_KEYWORDS.add("ON_FAILURE");
        RESERVED_KEYWORDS.add("OR");
        RESERVED_KEYWORDS.add("OUTER");
        RESERVED_KEYWORDS.add("OUTPUTTYPE");
        RESERVED_KEYWORDS.add("PAGES");
        RESERVED_KEYWORDS.add("PAGESIZE");
        RESERVED_KEYWORDS.add("PARTITION");
        RESERVED_KEYWORDS.add("PASS");
        RESERVED_KEYWORDS.add("PAUSE");
        RESERVED_KEYWORDS.add("PI");
        RESERVED_KEYWORDS.add("PLUS");
        RESERVED_KEYWORDS.add("PORT");
        RESERVED_KEYWORDS.add("POSITION");
        RESERVED_KEYWORDS.add("PPS");
        RESERVED_KEYWORDS.add("PRECISION");
        RESERVED_KEYWORDS.add("PREV");
        RESERVED_KEYWORDS.add("PRIMARY");
        RESERVED_KEYWORDS.add("PRIVILEGE");
        RESERVED_KEYWORDS.add("PRIVILEGES");
        RESERVED_KEYWORDS.add("QDURATION");
        RESERVED_KEYWORDS.add("QEND");
        RESERVED_KEYWORDS.add("QNODE");
        RESERVED_KEYWORDS.add("QNODES");
        RESERVED_KEYWORDS.add("QSTART");
        RESERVED_KEYWORDS.add("QTAGS");
        RESERVED_KEYWORDS.add("QTIME");
        RESERVED_KEYWORDS.add("QUERIES");
        RESERVED_KEYWORDS.add("QUERY");
        RESERVED_KEYWORDS.add("QUESTION");
        RESERVED_KEYWORDS.add("RAISE");
        RESERVED_KEYWORDS.add("RAND");
        RESERVED_KEYWORDS.add("RANGE");
        RESERVED_KEYWORDS.add("RATIO");
        RESERVED_KEYWORDS.add("READ");
        RESERVED_KEYWORDS.add("RECURSIVE");
        RESERVED_KEYWORDS.add("REGEXP");
        RESERVED_KEYWORDS.add("REDISTRIBUTE");
        RESERVED_KEYWORDS.add("REM");
        RESERVED_KEYWORDS.add("REPLACE");
        RESERVED_KEYWORDS.add("REPLICA");
        RESERVED_KEYWORDS.add("RESET");
        RESERVED_KEYWORDS.add("RESTORE");
        RESERVED_KEYWORDS.add("RESTRICT");
        RESERVED_KEYWORDS.add("RESUME");
        RESERVED_KEYWORDS.add("RETENTIONS");
        RESERVED_KEYWORDS.add("REVOKE");
        RESERVED_KEYWORDS.add("RIGHT");
        RESERVED_KEYWORDS.add("ROLLUP");
        RESERVED_KEYWORDS.add("ROW");
        RESERVED_KEYWORDS.add("ROWTS");
        RESERVED_KEYWORDS.add("RP");
        RESERVED_KEYWORDS.add("RSHIFT");
        RESERVED_KEYWORDS.add("S3_CHUNKPAGES");
        RESERVED_KEYWORDS.add("S3_COMPACT");
        RESERVED_KEYWORDS.add("S3_KEEPLOCAL");
        RESERVED_KEYWORDS.add("SCHEMALESS");
        RESERVED_KEYWORDS.add("SCORES");
        RESERVED_KEYWORDS.add("SEMI");
        RESERVED_KEYWORDS.add("SERVER_STATUS");
        RESERVED_KEYWORDS.add("SERVER_VERSION");
        RESERVED_KEYWORDS.add("SESSION");
        RESERVED_KEYWORDS.add("SET");
        RESERVED_KEYWORDS.add("SHOW");
        RESERVED_KEYWORDS.add("SINGLE_STABLE");
        RESERVED_KEYWORDS.add("SLASH");
        RESERVED_KEYWORDS.add("SLIDING");
        RESERVED_KEYWORDS.add("SLIMIT");
        RESERVED_KEYWORDS.add("SMA");
        RESERVED_KEYWORDS.add("SMALLINT");
        RESERVED_KEYWORDS.add("SMIGRATE");
        RESERVED_KEYWORDS.add("SNODE");
        RESERVED_KEYWORDS.add("SNODES");
        RESERVED_KEYWORDS.add("SOFFSET");
        RESERVED_KEYWORDS.add("SPLIT");
        RESERVED_KEYWORDS.add("STABLES");
        RESERVED_KEYWORDS.add("STAR");
        RESERVED_KEYWORDS.add("START");
        RESERVED_KEYWORDS.add("STATE");
        RESERVED_KEYWORDS.add("STATE_WINDOW");
        RESERVED_KEYWORDS.add("STATEMENT");
        RESERVED_KEYWORDS.add("STORAGE");
        RESERVED_KEYWORDS.add("STREAM");
        RESERVED_KEYWORDS.add("STREAMS");
        RESERVED_KEYWORDS.add("STRICT");
        RESERVED_KEYWORDS.add("STRING");
        RESERVED_KEYWORDS.add("STT_TRIGGER");
        RESERVED_KEYWORDS.add("SUBSCRIBE");
        RESERVED_KEYWORDS.add("SUBSCRIPTIONS");
        RESERVED_KEYWORDS.add("SUBSTR");
        RESERVED_KEYWORDS.add("SUBSTRING");
        RESERVED_KEYWORDS.add("SUBTABLE");
        RESERVED_KEYWORDS.add("SYSINFO");
        RESERVED_KEYWORDS.add("SYSTEM");
        RESERVED_KEYWORDS.add("TABLE_PREFIX");
        RESERVED_KEYWORDS.add("TABLE_SUFFIX");
        RESERVED_KEYWORDS.add("TABLES");
        RESERVED_KEYWORDS.add("TAG");
        RESERVED_KEYWORDS.add("TAGS");
        RESERVED_KEYWORDS.add("TBNAME");
        RESERVED_KEYWORDS.add("THEN");
        RESERVED_KEYWORDS.add("TIMES");
        RESERVED_KEYWORDS.add("TIMESTAMP");
        RESERVED_KEYWORDS.add("TIMEZONE");
        RESERVED_KEYWORDS.add("TINYINT");
        RESERVED_KEYWORDS.add("TO");
        RESERVED_KEYWORDS.add("TODAY");
        RESERVED_KEYWORDS.add("TOPIC");
        RESERVED_KEYWORDS.add("TOPICS");
        RESERVED_KEYWORDS.add("TRAILING");
        RESERVED_KEYWORDS.add("TRANSACTION");
        RESERVED_KEYWORDS.add("TRANSACTIONS");
        RESERVED_KEYWORDS.add("TRIGGER");
        RESERVED_KEYWORDS.add("TRIM");
        RESERVED_KEYWORDS.add("TRUE_FOR");
        RESERVED_KEYWORDS.add("TSDB_PAGESIZE");
        RESERVED_KEYWORDS.add("TSERIES");
        RESERVED_KEYWORDS.add("TSMA");
        RESERVED_KEYWORDS.add("TSMAS");
        RESERVED_KEYWORDS.add("TTL");
        RESERVED_KEYWORDS.add("UNION");
        RESERVED_KEYWORDS.add("UNSAFE");
        RESERVED_KEYWORDS.add("UNSIGNED");
        RESERVED_KEYWORDS.add("UNTREATED");
        RESERVED_KEYWORDS.add("USE");
        RESERVED_KEYWORDS.add("USER");
        RESERVED_KEYWORDS.add("USERS");
        RESERVED_KEYWORDS.add("USING");
        RESERVED_KEYWORDS.add("VALUE");
        RESERVED_KEYWORDS.add("VALUE_F");
        RESERVED_KEYWORDS.add("VALUES");
        RESERVED_KEYWORDS.add("VARBINARY");
        RESERVED_KEYWORDS.add("VARCHAR");
        RESERVED_KEYWORDS.add("VARIABLE");
        RESERVED_KEYWORDS.add("VARIABLES");
        RESERVED_KEYWORDS.add("VERBOSE");
        RESERVED_KEYWORDS.add("VGROUP");
        RESERVED_KEYWORDS.add("VGROUPS");
        RESERVED_KEYWORDS.add("VIEW");
        RESERVED_KEYWORDS.add("VIEWS");
        RESERVED_KEYWORDS.add("VNODE");
        RESERVED_KEYWORDS.add("VNODES");
        RESERVED_KEYWORDS.add("WAL");
        RESERVED_KEYWORDS.add("WAL_FSYNC_PERIOD");
        RESERVED_KEYWORDS.add("WAL_LEVEL");
        RESERVED_KEYWORDS.add("WAL_RETENTION_PERIOD");
        RESERVED_KEYWORDS.add("WAL_RETENTION_SIZE");
        RESERVED_KEYWORDS.add("WAL_ROLL_PERIOD");
        RESERVED_KEYWORDS.add("WAL_SEGMENT_SIZE");
        RESERVED_KEYWORDS.add("WATERMARK");
        RESERVED_KEYWORDS.add("WDURATION");
        RESERVED_KEYWORDS.add("WEND");
        RESERVED_KEYWORDS.add("WHEN");
        RESERVED_KEYWORDS.add("WINDOW");
        RESERVED_KEYWORDS.add("WINDOW_CLOSE");
        RESERVED_KEYWORDS.add("WINDOW_OFFSET");
        RESERVED_KEYWORDS.add("WITH");
        RESERVED_KEYWORDS.add("WRITE");
        RESERVED_KEYWORDS.add("WSTART");
    }

    /**
     * 字段名长度限制
     */
    private static final int MAX_LENGTH = 64;

    /**
     * 正则表达式：允许以字母、下划线或中文字符开头，后跟字母、数字、下划线或中文字符
     */
    private static final Pattern VALID_COLUMN_NAME_PATTERN = Pattern.compile("^[A-Za-z_\u4e00-\u9fa5][A-Za-z0-9_\u4e00-\u9fa5]*$");

    /**
     * 蛇形命名法正则表达式：允许以小写字母、下划线或中文字符开头，后跟小写字母、数字、下划线或中文字符
     */
    private static final Pattern SNAKE_CASE_PATTERN = Pattern.compile("^[a-z_\u4e00-\u9fa5][a-z0-9_\u4e00-\u9fa5]*$");

    public static void validateColumnName(String columnName) {
        // 1. 检查字段长度
        if (columnName.length() > MAX_LENGTH) {
            throw new ValidateCodeException(MsgUtils.getMessage(IotProductConstant.ERROR_VALIDATOR_LENGTH) + MAX_LENGTH + MsgUtils.getMessage(IotProductConstant.ERROR_VALIDATOR_LENGTH2));
        }

        // 2. 检查字段名是否合法（只包含字母、数字和下划线，且不能以数字开头）
        if (!VALID_COLUMN_NAME_PATTERN.matcher(columnName).matches()) {
            throw new ValidateCodeException(MsgUtils.getMessage(IotProductConstant.ERROR_VALIDATOR_ZW));
        }

        // 3. 检查字段名是否为保留关键字
        if (RESERVED_KEYWORDS.contains(columnName.toUpperCase())) {
            throw new ValidateCodeException(MsgUtils.getMessage(IotProductConstant.ERROR_VALIDATOR_KEYWORDS));
        }

        // 4. 检查字段名是否符合蛇形命名法
        if (!SNAKE_CASE_PATTERN.matcher(columnName).matches()) {
            throw new ValidateCodeException(MsgUtils.getMessage(IotProductConstant.ERROR_VALIDATOR_SERPENTINE));
        }
    }
}

