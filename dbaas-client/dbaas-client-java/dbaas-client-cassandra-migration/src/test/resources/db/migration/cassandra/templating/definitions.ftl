<#macro add_ttl_for_ak>
    <#if IS_AMAZON_KEYSPACES!false>
        WITH custom_properties = {'ttl': {'status': 'enabled'}}
    </#if>
</#macro>

<#macro add_compaction_for_c>
    <#if !(IS_AMAZON_KEYSPACES!false)>
        with compaction = {
        'class': 'LeveledCompactionStrategy',
        'unchecked_tombstone_compaction': 'true'
        }
    </#if>
</#macro>