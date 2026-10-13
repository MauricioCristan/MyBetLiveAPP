package com.mybetlive.ui
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
@Composable internal fun ExpandableFilters(selected:String,options:List<Pair<String,String>>,onSelected:(String)->Unit,
    enabled:Boolean=true,tagPrefix:String,icon:@Composable (String)->Unit) {
    val scope=rememberCoroutineScope()
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically){
        options.forEach{(id,label)->
            val requester=remember(id){BringIntoViewRequester()}
            FilterChip(selected=selected==id,onClick={onSelected(id)},enabled=enabled,
                label={Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)){
                    icon(id)
                    AnimatedVisibility(selected==id){LocalizedText(label,maxLines=1)}
                }},
                colors=FilterChipDefaults.filterChipColors(selectedContainerColor=Green.copy(alpha=.13f),selectedLabelColor=Green,labelColor=Muted),
                modifier=Modifier.testTag("$tagPrefix-$id").bringIntoViewRequester(requester)
                    .animateContentSize(finishedListener={_,_->if(selected==id)scope.launch{requester.bringIntoView()}})
                    .semantics{contentDescription=com.mybetlive.i18n.AppStrings.translate(label)})
        }
    }
}
