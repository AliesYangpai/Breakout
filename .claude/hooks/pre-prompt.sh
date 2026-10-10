#!/usr/bin/env bash
# UserPromptSubmit hook：检测用户输入是否涉及需求变更 / bug，命中则提示回流需求阶段。
# 输入：stdin JSON（含 .prompt 字段）；输出：命中时打印 systemMessage JSON，否则静默退出 0。
set -u

input="$(cat)"

prompt="$(printf '%s' "$input" | jq -r '.prompt // empty' 2>/dev/null)"

[ -z "$prompt" ] && exit 0

if printf '%s' "$prompt" | grep -Eiq 'bug|改需求|需求变更|新增需求|需求调整'; then
  printf '%s\n' '{"systemMessage":"检测到需求变更或 bug 相关输入。按研发流程需回流【需求阶段】，建议执行 /skill 01-requirement。"}'
fi

exit 0
