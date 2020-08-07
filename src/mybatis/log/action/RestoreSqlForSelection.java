package mybatis.log.action;

import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import mybatis.log.Icons;
import mybatis.log.tail.TailRunExecutor;
import mybatis.log.util.ConfigUtil;
import mybatis.log.util.PrintUtil;
import mybatis.log.util.RestoreSqlUtil;
import mybatis.log.util.StringConst;
import org.apache.commons.lang.StringUtils;

import java.awt.*;

/**
 * restore sql from selection
 *
 * @author ob
 */
public class RestoreSqlForSelection extends AnAction {
    private static String preparingLine = "";
    private static String parametersLine = "";
    private static boolean isEnd = false;

    public RestoreSqlForSelection() {
        super(() -> null, () -> null, Icons.MyBatisIcon);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        final Project project = e.getProject();
        if (project == null) return;
        CaretModel caretModel = e.getData(LangDataKeys.EDITOR).getCaretModel();
        Caret currentCaret = caretModel.getCurrentCaret();
        String sqlText = currentCaret.getSelectedText();
        ToolWindow toolWindow =
                ToolWindowManager.getInstance(project).getToolWindow(TailRunExecutor.TOOLWINDOWS_ID);
        if (!ConfigUtil.active || !toolWindow.isAvailable()) {
            new ShowLogInConsoleAction(project).showLogInConsole(project);
        }
        //激活Restore Sql tab
        toolWindow.activate(null);
        final String PREPARING = ConfigUtil.getPreparing(project);
        final String PARAMETERS = ConfigUtil.getParameters(project);
        if (StringUtils.isNotBlank(sqlText) && sqlText.contains(PREPARING) && sqlText.contains(PARAMETERS)) {
            String[] sqlArr = sqlText.split("\n");
            if (sqlArr != null && sqlArr.length >= 2) {
                for (int i = 0; i < sqlArr.length; ++i) {
                    String currentLine = sqlArr[i];
                    if (StringUtils.isBlank(currentLine)) {
                        continue;
                    }
                    if (currentLine.contains(PREPARING)) {
                        preparingLine = currentLine;
                        continue;
                    } else {
                        currentLine += "\n";
                    }
                    if (StringUtils.isEmpty(preparingLine)) {
                        continue;
                    }
                    if (currentLine.contains(PARAMETERS)) {
                        parametersLine = currentLine;
                    } else {
                        if (StringUtils.isBlank(parametersLine)) {
                            continue;
                        }
                        parametersLine += currentLine;
                    }
                    if (!parametersLine.endsWith("Parameters: \n") && !parametersLine.endsWith(
                            "null\n") && !RestoreSqlUtil.endWithAssembledTypes(parametersLine)) {
                        if (i == sqlArr.length - 1) {
                            PrintUtil.println(project, "Can't restore sql from selection.",
                                    PrintUtil.getOutputAttributes(null, Color.yellow));
                            PrintUtil.println(project, StringConst.SPLIT_LINE,
                                    ConsoleViewContentType.USER_INPUT);
                            this.reset();
                            break;
                        }
                        continue;
                    } else {
                        isEnd = true;
                    }
                    if (StringUtils.isNotEmpty(preparingLine) && StringUtils.isNotEmpty(parametersLine) && isEnd) {
                        int indexNum = ConfigUtil.getIndexNum(project);
                        String preStr = "--  " + indexNum + "  restore sql from selection  - ==>";
                        ConfigUtil.setIndexNum(project, ++indexNum);
                        PrintUtil.println(project, preStr, ConsoleViewContentType.USER_INPUT);
                        String restoreSql = RestoreSqlUtil.restoreSql(project, preparingLine, parametersLine);
                        PrintUtil.println(project, restoreSql, PrintUtil.getOutputAttributes(null, new Color(255,200,0)));//高亮显示
                        PrintUtil.println(project, StringConst.SPLIT_LINE, ConsoleViewContentType.USER_INPUT);
                        this.reset();
                    }
                }
            } else {
                PrintUtil.println(project, "Can't restore sql from selection.",
                        PrintUtil.getOutputAttributes(null, Color.yellow));
                PrintUtil.println(project, StringConst.SPLIT_LINE,
                        ConsoleViewContentType.USER_INPUT);
                this.reset();
            }
        } else {
            PrintUtil.println(project, "Can't restore sql from selection.",
                    PrintUtil.getOutputAttributes(null, Color.yellow));
            PrintUtil.println(project, StringConst.SPLIT_LINE, ConsoleViewContentType.USER_INPUT);
            this.reset();
        }
    }

    private void reset() {
        preparingLine = "";
        parametersLine = "";
        isEnd = false;
    }
}