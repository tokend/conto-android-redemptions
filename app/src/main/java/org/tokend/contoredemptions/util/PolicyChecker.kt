package org.tokend.contoredemptions.util

interface PolicyChecker {
    fun checkPolicy(policy: Int, mask: Int): Boolean {
        return policy and mask == mask
    }
}